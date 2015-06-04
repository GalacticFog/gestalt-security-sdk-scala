package com.galacticfog.gestalt.security.api

import com.fasterxml.jackson.core.JsonParseException
import com.galacticfog.gestalt.Gestalt
import com.galacticfog.gestalt.io.ConfigEntityReader
import com.galacticfog.gestalt.io.GestaltConfig.ConfigEntity
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Success, Try}

case class GestaltSecurityInitializationException(message: String, innerEx: Option[Throwable]) extends Throwable {
  override def getMessage = message
}

object Res {
  val ErrNotDirectory          = "Path is not a directory."
  val ErrFileNotFound          = "File not found."
  val ErrConfigNotFound        = "Could not find configuration in path."
  val ErrPathNotFound          = "Path not found."
  val ErrDirectoryNeedFile     = "Value is a directory, File required."
  val ErrGestaltConfigNotFound = "Gestalt security configuration not found."
  val ErrGestaltConfigInvalid  = "Invalid Gestalt security configuration data."
  val ErrMalformedJson         = "Malformed JSON."
}

case class GestaltSecurityConfig(protocol: Protocol,
                                 host: String,
                                 port: Int,
                                 apiKey: String,
                                 apiSecret: String,
                                 appId: Option[String]) extends ConfigEntity

object GestaltSecurityConfig {
  implicit val gestaltSecurityProtocolFormat = new Format[Protocol] {
    override def writes(p: Protocol): JsValue = JsString(p.toString)
    override def reads(json: JsValue): JsResult[Protocol] = {
      json match {
        case s: JsString =>
          s.value.toUpperCase match {
            case "HTTP" => JsSuccess(HTTP)
            case "HTTPS" => JsSuccess(HTTPS)
            case _ =>  JsError(ValidationError("Invalid security protocol",s))
          }
        case a => JsError(ValidationError("Gestalt security protocol must be a string", a))
      }
    }
  }

  implicit val gestaltSecurityConfigFormat: Format[GestaltSecurityConfig] = (
    (__ \ "protocol").format[Protocol] and
      (__ \ "hostname").format[String] and
      (__ \ "port").format[Int] and
      (__ \ "apiKey").format[String] and
      (__ \ "apiSecret").format[String] and
      (__ \ "appId").formatNullable[String]
    )(GestaltSecurityConfig.apply, unlift(GestaltSecurityConfig.unapply))

  private val configFileName = "gestalt-security.conf"

  val ePROTOCOL = "GESTALT_SECURITY_PROTOCOL"
  val eHOSTNAME = "GESTALT_SECURITY_HOSTNAME"
  val ePORT     = "GESTALT_SECURITY_PORT"
  val eKEY      = "GESTALT_SECURITY_KEY"
  val eSECRET   = "GESTALT_SECURITY_SECRET"
  val eAPPID    = "GESTALT_SECURITY_APPID"

  val eCONFIG     = "GESTALT_SECURITY_CONFIG"
  val eHOME       = "GESTALT_HOME"
  val eSEC_HOME   = "GESTALT_SECURITY_HOME"


  /**
   * Get the security config from the following sources, in the following order:
   * 1) meta, via the supplied client if not None
   * 2) environment variables
   * 3) the filesystem
   * @param meta
   * @return Some(config) if a security configuration was found, None otherwise
   */
  def getSecurityConfig(meta: Option[Gestalt]): Option[GestaltSecurityConfig] = {
    getSecurityConfigFromMeta(meta) orElse getSecurityConfigFromEnvFile
  }

  def getSecurityConfigFromMeta(meta: Option[Gestalt]): Option[GestaltSecurityConfig] = {
    for {
      m <- meta
      str <- m.getConfig("authentication").toOption
      json <- Try(Json.parse(str)).toOption
      config <- json.asOpt[GestaltSecurityConfig]
    } yield config
  }

  def getSecurityConfigFromEnv: Option[GestaltSecurityConfig] = {
    println("checking environment for Gestalt security config")
    for {
      proto  <- getEnv(ePROTOCOL) orElse Some("http") map checkProtocol
      host   <- getEnv(eHOSTNAME)
      port   <- getEnv(ePORT)
      key    <- getEnv(eKEY)
      secret <- getEnv(eSECRET)
      appId   = getEnv(eAPPID)
    } yield GestaltSecurityConfig(protocol=proto, host=host, port=port.toInt, apiKey=key, apiSecret=secret, appId=appId)
  }

  def getSecurityConfigFromFile: Option[GestaltSecurityConfig] = {
    println("checking filesystem for Gestalt security config")
    val reader = new ConfigEntityReader[GestaltSecurityConfig]
    val data = loadFile(resolvePath) match {
      case Success(file) => file
      case Failure(ex) => throw ex
    }
    reader.read(data) match {
      case Success(context) => Some(context)
      case Failure(ex) => ex match {
        case jpe: JsonParseException =>
          throw error(Res.ErrMalformedJson, Some(jpe))
        case iae: IllegalArgumentException =>
          throw error(Res.ErrGestaltConfigInvalid, Some(iae))
      }
    }
  }

  def getSecurityConfigFromEnvFile: Option[GestaltSecurityConfig] = {
    getSecurityConfigFromEnv orElse getSecurityConfigFromFile
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  // private stuff
  ///////////////////////////////////////////////////////////////////////////////////////////////////

  private def isDir(path: String) = new java.io.File(path).isDirectory

  private def mkString(path: String) = scala.io.Source.fromFile(path).mkString

  private def exists(path: String) = new java.io.File(path).exists

  private def strip(s: String) = if (s.trim.endsWith("/")) s.dropRight(1) else s

  private def confname(ps: String*): String = {
    val p = if (ps.size > 1) ps reduce { (a, b) => strip(a) + "/" + strip(b) }
    else if (ps.size == 1) strip(ps(0))
    else ""
    "%s/%s".format(p, configFileName)
  }

  private def curdir = scala.util.Properties.userDir

  private def getEnv(name: String): Option[String] = scala.util.Properties.envOrNone(name)

  private def checkProtocol(proto: String): Protocol = proto match {
    case "HTTP" => HTTP
    case "http" => HTTP
    case "HTTPS" => HTTPS
    case "https" => HTTPS
    case _ => throw new RuntimeException("Invalid protocol for Gestalt security")
  }

  private def resolveGestaltConfig(config: String): Try[String] = {
    if (!exists(config))
      Failure(error("File not found: " + config))
    else if (isDir(config) && exists(confname(config))) Success(confname(config))
    else Success(config)
  }

  private def resolveGestaltHome(home: String): Try[String] = {
    import Res._
    if (!exists(home)) {
      Failure(error(ErrPathNotFound))
    }
    else if (!isDir(home)) {
      Failure(error(ErrNotDirectory))
    }
    else {
      if (exists(confname(home))) Success(confname(home))
      else if (exists(confname(home, "conf"))) Success(confname(home, "conf"))
      else Failure(error(ErrConfigNotFound))
    }
  }

  private def resolvePath: Try[String] = {
    if (getEnv( eCONFIG ).isDefined)        resolveGestaltConfig(getEnv( eCONFIG ).get)
    else if (getEnv( eHOME ).isDefined)     resolveGestaltHome(getEnv( eHOME ).get)
    else if (getEnv( eSEC_HOME ).isDefined) resolveGestaltHome(getEnv( eSEC_HOME ).get)
    else if (exists(confname( curdir )))    Success(confname(curdir))
    else Failure(error(Res.ErrGestaltConfigNotFound, None))
  }

  private[gestalt] def loadFile(path: Try[String]): Try[String] = {
    path map { p =>
      println(s"using file => $p")
      if (!exists(p)) throw error("%s, %s".format(p,  Res.ErrFileNotFound))
      else if (isDir(p)) {
        if (exists(confname(p))) mkString(confname(p))
        else throw error("%s, %s".format(p, Res.ErrDirectoryNeedFile))
      }
      else mkString(p)
    }
  }

  private def error(message: String, inner: Option[Throwable] = None) =
    GestaltSecurityInitializationException(message, inner)

  private def invalidVar(name: String, value: String, msg: String) =
    "%s=%s : %s".format(name, value, msg)
}


