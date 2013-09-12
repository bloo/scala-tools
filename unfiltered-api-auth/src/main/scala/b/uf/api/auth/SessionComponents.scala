package b.uf.api.auth

/**
 * cake pattern notes here:
 *  
 * http://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth/
 */ 

trait TokenComponent[T] extends b.uf.api.ResourceAuthComponent[T] {
    def tokenService: TokenService
    trait TokenService {
        def lookup(principal: String, secret: String): Option[T]
    }
}

trait SessionComponent[T,S] {
    def sessionService: SessionService
    trait SessionService {
        def create(token: T, remember: Boolean): S
        def remove(sess: S): Boolean
        def local: Option[S]
        def localToken: Option[T]
        def get(id: String): Option[S]
    }
}



