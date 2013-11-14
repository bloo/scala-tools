package b.storage.s3

import b.storage._
import java.util.UUID
import org.apache.commons.io.IOUtils

/**
 * JetS3t based implementation of Amazon AWS's S3 RESTful API.
 * 
 * http://www.jets3t.org/toolkit/configuration.html
 */
trait S3StorageComponent[T] extends StorageComponent[T] {

    override def storageService = _ss

    val awsScheme: String // http | https
    val awsHost: String
    val rootBucket: String
    val objectBucket: String
    def awsCreds: (String, String)

    def persist(om: ObjectMeta[T]): Boolean
    def lookup(id: String): Option[ObjectMeta[T]]
    def auth(owner: T): Boolean
    
    private lazy val _ss = new StorageService[T] with b.log.Logger {

        import org.jets3t.service.security.AWSCredentials
        import org.jets3t.service.impl.rest.httpclient.RestS3Service
        import org.jets3t.service.model.S3Object
        import org.jets3t.service.acl.AccessControlList
	    import java.io.{ InputStream, FileOutputStream, File }

        lazy val aws = awsCreds
        lazy val jet = new RestS3Service(new AWSCredentials(aws._1, aws._2))

        def authorized(owner: T) = auth(owner)
        
    	def request(owner: T, name: String): Option[ObjectMeta[T]] = {
            if (auth(owner)) {
	        	val id = UUID.randomUUID.toString.replaceAll("-", "")
	        	val url = new java.net.URL("%s://%s/%s/%s/%s-%s" format
	        	        (awsScheme, awsHost, rootBucket, objectBucket, id, name))
	        	val size = 0
	        	val status = ObjectStatus.ready
	    	    val om = ObjectMeta[T](owner, id, name, url, size, status)
	    	    if (persist(om)) Some(om) else None
            } else None
    	}

        /**
         * http://www.jets3t.org/toolkit/code-samples.html#uploading
         * 
         * setting data with File or String allows S3Object to calculate
         * Content-Length. setting directly from InputStream would require
         * us to calculate length, so would rather write to file system
         * than store in memory
         */
    	def store(owner: T, id: String, is: InputStream): Option[ObjectMeta[T]] = if (authorized(owner)) find(id) match {
            case Some(om) => {
                
                // upload stream to temp file
                // so we can calculate content-length
                //
	    	    val file = File.createTempFile("b.common.storage.s3.storage-service", ".dat")
	   			val out = new FileOutputStream(file);
	    	    IOUtils.copy(is, out)
	    	    IOUtils closeQuietly out
                IOUtils closeQuietly is

                // create S3 object to put up into AWS
                //
                val s3obj = new S3Object(om.name)                
                s3obj.setDataInputFile(file)
                s3obj.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ)
                
                // PUT and get resulting meta-data
                //
                val rez = jet.putObject(_bucket(om.url), s3obj) // blocks!
                val len = rez.getContentLength
                
                // create new ObjectMeta from result, persist internally, and return
                //
                val newOm = ObjectMeta[T](owner, id, om.name, om.url, len, ObjectStatus.completed)
                if (persist(newOm)) Some(newOm) else None
            }
            case None => None
        } else None

    	/**
    	 * http://www.jets3t.org/toolkit/code-samples.html#deleting
    	 */
        def delete(owner: T, id: String): Boolean = if (authorized(owner)) find(id) match {
    	    case Some(om) => jet.deleteObject(_bucket(om.url), om.name); true
    	    case None => false
        } else false
        
    	def find(id: String): Option[ObjectMeta[T]] = lookup(id)
    	
    	def _bucket(url: java.net.URL): String = {
    		import org.apache.commons.io.FilenameUtils
    		// convert /foo/bar/baz.txt => foo/bar
    		FilenameUtils.getPath(url.getPath)
    	}    	
    }
}