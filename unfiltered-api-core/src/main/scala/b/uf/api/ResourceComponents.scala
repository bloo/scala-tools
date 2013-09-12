
package b.uf.api

import unfiltered.request._

trait ResourceAuthComponent[T] {
    def authService: AuthService[T]
    trait AuthService[T] {
        //def realm: String
        def authenticate[X](req: unfiltered.request.HttpRequest[X]): Option[T]
    }
}
