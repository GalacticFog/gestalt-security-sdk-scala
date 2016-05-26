package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.fasterxml.jackson.core.JsonParseException
import com.galacticfog.gestalt.io.ConfigEntityReader
import com.galacticfog.gestalt.io.GestaltConfig.ConfigEntity
import play.api.Logger
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
  val ErrGestaltConfigNotFound = "A Gestalt security configuration file could not found."
  val ErrGestaltConfigInvalid  = "Invalid Gestalt security configuration data: %s"
  val ErrMalformedJson         = "Malformed JSON: %s"
}

sealed trait GestaltSecurityMode {
  def label: String
}
final case object DELEGATED_SECURITY_MODE extends GestaltSecurityMode {val label = "DELEGATED"}
final case object FRAMEWORK_SECURITY_MODE extends GestaltSecurityMode {val label = "FRAMEWORK"}

case class GestaltSecurityConfig(mode: GestaltSecurityMode,
                                 protocol: Protocol,
                                 hostname: String,
                                 port: Int,
                                 apiKey: String,
                                 apiSecret: String,
                                 appId: Option[UUID]) extends ConfigEntity {
  def isWellDefined: Boolean = !hostname.isEmpty && !apiKey.isEmpty && !apiSecret.isEmpty && port > 0 && (mode match {
    case DELEGATED_SECURITY_MODE =>
      appId.isDefined
    case FRAMEWORK_SECURITY_MODE =>
      true
  })
}

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

  implicit val gestaltSecurityConfigWrites = new Writes[GestaltSecurityConfig]{
    override def writes(o: GestaltSecurityConfig): JsValue = Json.obj(
      "protocol"  -> o.protocol,
      "hostname"  -> o.hostname,
      "port"      -> o.port,
      "apiKey"    -> o.apiKey,
      "apiSecret" -> o.apiSecret,
      "appId"     -> o.appId
    )
  }

  implicit val gestaltSecurityConfigReads = new Reads[GestaltSecurityConfig]{
    override def reads(json: JsValue): JsResult[GestaltSecurityConfig] = {
      val delegated = for {
        protocol <- (json \ "protocol").validate[Protocol]
        hostname <- (json \ "hostname").validate[String]
        port <- (json \ "port").validate[Int]
        appId <- (json \ "appId").validate[UUID]
        apiKey <- (json \ "apiKey").validate[String]
        apiSecret <- (json \ "apiSecret").validate[String]
      } yield GestaltSecurityConfig(DELEGATED_SECURITY_MODE,protocol,hostname,port,appId = Some(appId),apiKey = apiKey, apiSecret = apiSecret)
      delegated orElse(for {
        protocol <- (json \ "protocol").validate[Protocol]
        hostname <- (json \ "hostname").validate[String]
        port <- (json \ "port").validate[Int]
        apiKey <- (json \ "apiKey").validate[String]
        apiSecret <- (json \ "apiSecret").validate[String]
      } yield GestaltSecurityConfig(FRAMEWORK_SECURITY_MODE,protocol,hostname,port,appId = None,apiKey = apiKey,apiSecret = apiSecret))
    }
  }

  private val configFileName = "gestalt-security.conf"

  val ePROTOCOL = "GESTALT_SECURITY_PROTOCOL"
  val eHOSTNAME = "GESTALT_SECURITY_HOSTNAME"
  val ePORT     = "GESTALT_SECURITY_PORT"
  val eKEY      = "GESTALT_SECURITY_KEY"
  val eSECRET   = "GESTALT_SECURITY_SECRET"
  val eAPPID    = "GESTALT_SECURITY_APPID"

  val eCONFIG     = "GESTALT_SECURITY_CONFIG"

  /**
   * Get the security config from the following sources, in the following order:
   * 1) environment variables
   * 2) the filesystem
   * @return Some(config) if a security configuration was found, None otherwise
   */
  def getSecurityConfig: Option[GestaltSecurityConfig] = {
    getSecurityConfigFromEnv orElse getSecurityConfigFromFile
  }

  def getSecurityConfigFromEnv: Option[GestaltSecurityConfig] = {
    Logger.info("> checking environment for Gestalt security config")
    val delegated = for {
      proto  <- getEnv(ePROTOCOL) orElse Some("http") map checkProtocol
      host   <- getEnv(eHOSTNAME)
      port   <- getEnv(ePORT) flatMap {s => Try{s.toInt}.toOption}
      key    <- getEnv(eKEY)
      secret <- getEnv(eSECRET)
      appId  <- getEnv(eAPPID) flatMap {s => Try{UUID.fromString(s)}.toOption}
    } yield GestaltSecurityConfig(mode=DELEGATED_SECURITY_MODE, protocol=proto, hostname=host, port=port, apiKey=key, apiSecret=secret, appId=Some(appId))
    delegated.orElse(for {
      proto  <- getEnv(ePROTOCOL) orElse Some("http") map checkProtocol
      host   <- getEnv(eHOSTNAME)
      port   <- getEnv(ePORT) flatMap {s => Try{s.toInt}.toOption}
      key    <- getEnv(eKEY)
      secret <- getEnv(eSECRET)
    } yield GestaltSecurityConfig(mode=FRAMEWORK_SECURITY_MODE, protocol=proto, hostname=host, port=port, apiKey=key, apiSecret=secret, None))
  }

  def getSecurityConfigFromFile: Option[GestaltSecurityConfig] = {
    Logger.info("> checking filesystem for Gestalt security config")
    val reader = new ConfigEntityReader[GestaltSecurityConfig]
    val securityContext = for {
      path <- resolvePath
      data <- loadFile(path)
      context <- reader.read(data)
    } yield context
    securityContext match {
      case Success(c) => Some(c)
      case Failure(ex) => ex match {
        case jpe: JsonParseException =>
          Logger.info(Res.ErrMalformedJson.format(jpe.getMessage))
          None
        case iae: IllegalArgumentException =>
          Logger.info(Res.ErrGestaltConfigInvalid.format(iae.getMessage))
          None
      }
    }
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

  private def resolvePath: Try[String] = {
    if (getEnv( eCONFIG ).isDefined)        resolveGestaltConfig(getEnv( eCONFIG ).get)
    else if (exists(confname( curdir )))    Success(confname(curdir))
    else Failure(error(Res.ErrGestaltConfigNotFound, None))
  }

  private[gestalt] def loadFile(path: String): Try[String] = {
    Try {
      Logger.info(s"> loading file: $path")
      if (!exists(path)) throw error("%s, %s".format(path, Res.ErrFileNotFound))
      else if (isDir(path)) {
        if (exists(confname(path))) mkString(confname(path))
        else throw error("%s, %s".format(path, Res.ErrDirectoryNeedFile))
      }
      else mkString(path)
    }
  }

  private def error(message: String, inner: Option[Throwable] = None) =
    GestaltSecurityInitializationException(message, inner)

  private def invalidVar(name: String, value: String, msg: String) =
    "%s=%s : %s".format(name, value, msg)
}


