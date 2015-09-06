package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.User

import views._

object Application extends Controller {
  
  def index = Action { implicit request =>
    
    Ok(views.html.index())
  }

  def messaging = Action { implicit request =>
    Ok(views.html.messaging.index())
  }

  def mobile = Action { implicit request =>
    Ok(views.html.mobile.index())
  }
  
  def consent(backend : String) = Action { implicit request =>      
    Ok(views.html.consent.index(backend))
  }
  
  def theme(theme: String) = Action { implicit request =>
    Redirect(routes.Application.index).withSession("theme" -> theme) 
  }
  
  // -- Authentication

  val loginForm = Form(
    tuple(
      "username" -> text(minLength = 1),
      "password" -> text(minLength = 1)
    ) verifying ("Invalid username or password", result => result match {
      case (username, password) => User.authenticate(username, password).isDefined
    })
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(loginForm));
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession("username" -> user._1)
    )
  }
  
  def authenticateSwisscom() = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.mock.sso.SwisscomLogin(formWithErrors))},
        user => Redirect(routes.ConsentMock.index())
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }
  
  def getLoggedUser = { username =>
	  User.findByUsername(username)
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.ConsentVerifyAddress.getOauthProviders,
        routes.javascript.Online3rdParty.segment,
        routes.javascript.Online3rdParty.simcards,
        routes.javascript.Online3rdParty.birthdate,
        routes.javascript.Online3rdParty.retention,
        routes.javascript.Online3rdParty.eligibility,
        routes.javascript.Online3rdParty.order,
        routes.javascript.Location.getDeviceLocation,
        routes.javascript.PaymentsFlowSelfcare.savePaymentAmount
        )
      ).as("text/javascript")
  }
}

/**
 * Provide security features
 */
trait Secured {
  
  /**
   * Retrieve the connected user username.
   */
  private def username(request: RequestHeader) = request.session.get("username")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
  
  // --
  
  /** 
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }
  
  def IsAuthenticatedUser(f: User => Request[AnyContent] => Result) = IsAuthenticated { username => request =>
	  User.findByUsername(username).map { user =>
	    f(user)(request)
	  }.getOrElse(onUnauthorized(request))
  }
 
}