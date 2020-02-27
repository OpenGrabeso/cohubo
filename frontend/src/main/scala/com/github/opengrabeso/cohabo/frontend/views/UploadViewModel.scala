package com.github.opengrabeso.cohabo
package frontend
package views

import org.scalajs.dom._
import io.udash.properties.HasModelPropertyCreator
import io.udash.utils.FileUploader.{FileUploadModel, FileUploadState}

class UploadViewModel(
  val state: FileUploadModel = new FileUploadModel(Seq.empty, FileUploadState.NotStarted, 0, 0, response = None),
  val selectedFiles: Seq[File] = Seq.empty,
  //val uploadedFiles: Seq[UploadedFile] = Seq.empty
)

object UploadViewModel extends HasModelPropertyCreator[UploadViewModel]