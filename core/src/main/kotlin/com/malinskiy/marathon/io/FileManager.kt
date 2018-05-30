package com.malinskiy.marathon.io

import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toTestName
import org.apache.commons.io.filefilter.SuffixFileFilter
import java.io.File
import java.io.FileFilter
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths.get

class FileManager(private val output: File) {
    fun createFile(fileType: FileType, pool: DevicePoolId, device: Device, test: Test): File {
        val directory = createDirectory(fileType, pool, device)
        val filename = createFilename(test, fileType)
        return createFile(directory, filename)
    }

    fun createFile(fileType: FileType, pool: DevicePoolId, device: Device): File {
        val directory = createDirectory(fileType, pool)
        val filename = createFilename(device, fileType)
        return createFile(directory, filename)
    }

    fun getFiles(fileType: FileType, pool: DevicePoolId): Array<File> {
        val fileFilter: FileFilter = SuffixFileFilter(fileType.suffix)
        val deviceDirectory = get(output.absolutePath, fileType.dir, pool.name).toFile()
        return deviceDirectory.listFiles(fileFilter)
    }

    fun getTestResultFilesForDevice(pool: DevicePoolId, device: Device): Array<File> {
        val path = getDirectory(FileType.TEST_RESULT, pool, device)
        return path.toFile().listFiles()
    }

    private fun createDirectory(fileType: FileType, pool: DevicePoolId, device: Device): Path =
            createDirectories(getDirectory(fileType, pool, device))

    private fun createDirectory(fileType: FileType, pool: DevicePoolId): Path =
            createDirectories(getDirectory(fileType, pool))

    private fun getDirectory(fileType: FileType, pool: DevicePoolId, device: Device): Path =
            get(output.absolutePath, fileType.dir, pool.name, device.serialNumber)

    private fun getDirectory(fileType: FileType, pool: DevicePoolId): Path =
            get(output.absolutePath, fileType.dir, pool.name)

    private fun createFile(directory: Path, filename: String): File = File(directory.toFile(), filename)

    private fun createFilename(test: Test, fileType: FileType): String = "${test.toTestName()}.${fileType.suffix}"
    private fun createFilename(device: Device, fileType: FileType): String = "${device.serialNumber}.${fileType.suffix}"
}
