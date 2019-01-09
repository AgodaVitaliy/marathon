package com.malinskiy.marathon.ios.cmd.remote

import ch.qos.logback.classic.Level
import com.malinskiy.marathon.log.MarathonLogging
import kotlinx.coroutines.*
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.LoggerFactory
import net.schmizz.sshj.connection.ConnectionException
import net.schmizz.sshj.transport.TransportException
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.RuntimeException
import java.net.InetAddress
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

private const val DEFAULT_PORT = 22
private const val SLEEP_DURATION_MILLIS = 15L
private const val SSH_SERVER_KEEPALIVE_INTERVAL_MILLIS = 5000

class SshjCommandExecutor(deviceContext: CoroutineContext,
                          udid: String,
                          val hostAddress: InetAddress,
                          val remoteUsername: String,
                          val remotePrivateKey: File,
                          val port: Int = DEFAULT_PORT,
                          val knownHostsPath: File? = null,
                          verbose: Boolean = false) : CommandExecutor, CoroutineScope {

    override val coroutineContext: CoroutineContext = newSingleThreadContext("$udid-ssh")
    private val ssh: SSHClient

    init {
        val config = DefaultConfig()
        val loggerFactory = object : LoggerFactory {
            override fun getLogger(clazz: Class<*>?): Logger {
                val name = clazz?.simpleName ?: SshjCommandExecutor::class.java.simpleName
                return MarathonLogging.logger(
                    name = name,
                    level = if (verbose || name == "Heartbeater") {
                        Level.DEBUG
                    } else {
                        Level.ERROR
                    }
                )
            }

            override fun getLogger(name: String?): Logger = MarathonLogging.logger(
                name = name ?: "",
                level = if (verbose) {
                    Level.DEBUG
                } else {
                    Level.ERROR
                }
            )
        }
        config.loggerFactory = loggerFactory

        ssh = SSHClient(config)
        ssh.connection.keepAlive.keepAliveInterval = SSH_SERVER_KEEPALIVE_INTERVAL_MILLIS / 1000
        knownHostsPath?.let { ssh.loadKnownHosts(it) }
        ssh.loadKnownHosts()
        val keys = ssh.loadKeys(remotePrivateKey.path)
        ssh.connect(hostAddress, port)
        ssh.authPublickey(remoteUsername, keys)
    }

    private val logger by lazy {
        MarathonLogging.logger(SshjCommandExecutor::class.java.simpleName)
    }

    override fun startSession(command: String): CommandSession {
        return SshjCommandSession(command, ssh)
    }

    override fun disconnect() {
        if (ssh.isConnected) {
            try {
                ssh.disconnect()
            } catch (e: IOException) {
                logger.warn("Error disconnecting $e")
            }
        }
    }

    private class OutputTimeoutException: RuntimeException()

    override suspend fun exec(command: String, timeoutMillis: Long, testOutputTimeoutMillis: Long, onLine: (String) -> Unit): Int? {
        val session = try {
            startSession(command)
        } catch (e: ConnectionException) {
            logger.error("Unable to start a remote shell session")
            throw e
        } catch (e: TransportException) {
            logger.error("Unable to start a remote shell session")
            throw e
        }

        val startTime = System.currentTimeMillis()
        logger.trace("Execution starts at ${startTime}ms")
        logger.trace(command)

        try {
            val executionTimeout = if (timeoutMillis == 0L) Long.MAX_VALUE else timeoutMillis
            withTimeout(executionTimeout) {

                val timeoutWaiter = SshjCommandOutputWaiterImpl(testOutputTimeoutMillis, SLEEP_DURATION_MILLIS)
                val isSessionReadable = { session.isOpen and !session.isEOF }

                awaitAll(
                    async(CoroutineName("stdout reader")) {
                        readLines(session.inputStream,
                            isSessionReadable,
                            {
                                timeoutWaiter.update()
                                onLine(it)
                            }
                        )
                    },
                    async(CoroutineName("stderr reader")) {
                        readLines(session.errorStream,
                            isSessionReadable,
                            {
                                timeoutWaiter.update()
                                onLine(it)
                            }
                        )
                    },
                    async(CoroutineName("Timeout waiter")) {
                        while (isActive and isSessionReadable()) {
                            if (timeoutWaiter.isExpired) {
                                throw OutputTimeoutException()
                            }
                            timeoutWaiter.wait()
                        }
                    }
                )
            }
        } catch (e: TimeoutCancellationException) {
            try {
                session.kill()
            } catch (e: TransportException) {
            }

            throw TimeoutException(e.message)
        } catch (e: OutputTimeoutException) {
            try {
                session.kill()
            } catch (e: TransportException) {
            }

            throw SshjCommandUnresponsiveException("Remote command \n\u001b[1m$command\u001b[0mdid not send any output over ${testOutputTimeoutMillis}ms")
        } finally {
            try {
                session.close()
            } catch (e: IOException) {
            } catch (e: TransportException) {
            } catch (e: ConnectionException) {
            }
        }
        logger.trace("Execution completed after ${System.currentTimeMillis() - startTime}ms")
        return session.exitStatus
    }

    private suspend fun readLines(inputStream: InputStream, canRead: () -> Boolean, onLine: (String) -> Unit) {
        SshjCommandOutputLineBuffer(onLine).use { lineBuffer ->
            val byteArray = ByteArray(16384)
            while (isActive) {
                val available = inputStream.available()
                // available value is expected to indicate an estimated number of bytes
                // that can be read without blocking (actual count may be smaller).
                //
                // when requesting a zero length, reading from sshj's [ChannelInputStream]
                // blocks. to accurately handle no output timeout, check if session has
                // received EOF.
                //
                val count = when {
                    available > 0 -> inputStream.read(byteArray, 0, min(available, byteArray.size))
                    else -> 0
                }
                // if there was nothing to read
                if (count == 0) {
                    // if session received EOF or has been closed, reading stops
                    if (!canRead()) {
                        logger.trace("Remote command output completed")
                        break
                    }
                    // sleep for a moment
                    delay(SLEEP_DURATION_MILLIS)
                } else {
                    lineBuffer.append(byteArray, count)
                }

                // immediately send any full lines for parsing
                lineBuffer.flush()
            }
        }
    }

    override fun exec(command: String, timeoutMillis: Long, testOutputTimeoutMillis: Long): CommandResult {
        val lines = arrayListOf<String>()
        val exitStatus =
                runBlocking(coroutineContext + CoroutineName("blocking exec")) {
                    exec(command, timeoutMillis, testOutputTimeoutMillis) { lines.add(it) }
                }
        return CommandResult(lines.joinToString("\n"), "", exitStatus ?: 1)
    }
}
