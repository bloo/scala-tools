package b.storage

object ObjectStatus extends Enumeration {
    type ObjectStatus = Value
    val ready, uploading, completed, error = Value
}

import ObjectStatus._
case class ObjectMeta[T](
		val owner: T,
        val id: String,
        val name: String,
        val url: java.net.URL,
        val size: Long,
        val status: ObjectStatus,
        val error: Option[String] = None)

trait StorageComponent[T] {

    def storageService: StorageService[T]

    trait StorageService[T] {
        def authorized(owner: T): Boolean
        def request(owner: T, name: String): Option[ObjectMeta[T]]
        def store(owner: T, id: String, is: java.io.InputStream): Option[ObjectMeta[T]]
        def delete(owner: T, id: String): Boolean
        def find(id: String): Option[ObjectMeta[T]]
    }
}
