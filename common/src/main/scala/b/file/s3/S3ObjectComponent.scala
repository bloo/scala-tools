package b.file.s3

import b.file._

trait S3ObjectComponent[T] extends FileComponent[T] {

    def fileService = _fs
    def awsService: AwsService[T]
    
    case class AwsCreds(val accessId: String, val secretKey: String)
    
    trait AwsService[T] {
        def getRootBucket: String
	    def getFileBucket(owner: T, objectType: String, name: String): String
	    def getAwsCreds: AwsCreds
    }
    
    private lazy val _fs = new FileService[T] with b.common.Logger {
    	
        def authorized(owner: T, objectType: String): Boolean = {
    	    false
    	}
        
    	def create(owner: T, objectType: String, name: String): FileMeta = {
        	val id = "id"
        	val url = "https://s3/%s/%s" format (objectType, name)
        	val size = 0
        	val status = FileStatus.ready
        	val error = None
    	    FileMeta(id, name, url, size, status, error)
    	}

    	def status(id: String): Option[FileMeta] = {
    	    None
    	}
    	
        def destroy(id: String): Boolean = {
            false
        }
    }
}