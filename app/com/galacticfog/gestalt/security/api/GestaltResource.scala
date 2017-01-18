package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.patch.PatchOp

import scala.language.experimental.macros
import play.api.libs.json.{JsNull, JsValue, Json}

import scala.concurrent.Future
import reflect.runtime.{universe => ru}
import scala.reflect.ClassTag

abstract class GestaltResource {
  def id: UUID
  def name: String
  def href: String
  def description: Option[String]

  def getLink(): ResourceLink = ResourceLink(id = id, name = name, href = href, properties = None)
}

case class ResourceLink(id: UUID, name: String, href: String, properties: Option[Map[String,String]] = None)

trait PatchSupport[A <: PatchSupport[A]] {
  import PatchSupport._

  def update(elems: (Symbol, JsValue)*)(implicit client: GestaltSecurityClient, fjs: play.api.libs.json.Reads[A], m: reflect.Manifest[A]): Future[A] = {
    PatchSupport.updateImpl[A](this.asInstanceOf[A], elems: _*)
  }
}

object PatchSupport {
  val REMOVE = JsNull

  def updateImpl[A: ClassTag](orig: A, elems: (Symbol, JsValue)*)(implicit client: GestaltSecurityClient, fjs: play.api.libs.json.Reads[A], m: reflect.Manifest[A], typeTag: ru.TypeTag[A]): Future[A] = {
    val patches = genPatch[A](orig, elems:_*)
    client.patch[A](
      orig.asInstanceOf[GestaltResource].href,
      Json.toJson(patches)
    )
  }

  def genPatch[A: ClassTag](orig: A, elems: (Symbol, JsValue)*)(implicit typeTag: ru.TypeTag[A]): Seq[PatchOp] = {
    val m: ru.Mirror = ru.runtimeMirror(orig.getClass.getClassLoader)
    val im = m.reflect(orig)
    val patches = elems.foldLeft(Map[String,PatchOp]()) {
      (patches, elem) =>
        val fieldName = elem._1.name
        if (patches.contains(fieldName)) throw new RuntimeException(
          s"invalid update: field ${fieldName} was provided multiple times"
        )
        val newVal = elem._2
        val symb = ru.typeOf[A].decl(ru.TermName(fieldName))
        if (symb == ru.NoSymbol) {
          throw new RuntimeException(s"invalid update: field ${fieldName} does not exist in class")
        }
        val isOptionField = symb.typeSignature.resultType <:< ru.typeOf[Option[_]]
        val curVal = im.reflectField(symb.asTerm).get
        val patch: PatchOp = newVal match {
          case REMOVE =>
            if (!isOptionField) throw new RuntimeException(s"invalid update: JsNull passed, but field ${fieldName} is not Option")
            PatchOp(op = "remove", path = s"/${fieldName}", value = None)
          case js: JsValue => curVal match {
            case opt if isOptionField && opt.asInstanceOf[Option[_]].isEmpty =>
              PatchOp(op = "add", path = s"/${fieldName}", value = Some(js))
            case _ =>
              PatchOp(op = "replace", path = s"/${fieldName}", value = Some(js))
          }
        }
        patches + (fieldName -> patch)
    }
    patches.values.toSeq
  }
}
