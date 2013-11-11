package b.file

object FileStatus extends Enumeration {
    type FileStatus = Value
    val ready, uploading, completed, error = Value
}

import FileStatus._
case class FileMeta(
        val id: String,
        val name: String,
        val url: String,
        val size: Int,
        val status: FileStatus, val error: Option[String])

trait FileComponent[T] {

    def fileService: FileService[T]

    trait FileService[T] {
        def authorized(owner: T, objectType: String): Boolean
        def create(owner: T, objectType: String, name: String): FileMeta
        def status(id: String): Option[FileMeta]
        def destroy(id: String): Boolean
    }
}
