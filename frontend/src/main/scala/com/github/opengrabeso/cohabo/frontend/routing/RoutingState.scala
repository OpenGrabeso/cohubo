package com.github.opengrabeso.cohabo
package frontend.routing

import common.model._
import io.udash._

/**
  * add any new state into:
-  [StatesToViewFactoryDef]
-  [RoutingRegistryDef]
  */

sealed abstract class RoutingState(val parentState: Option[ContainerRoutingState]) extends State {
  override type HierarchyRoot = RoutingState
}

sealed abstract class ContainerRoutingState(parentState: Option[ContainerRoutingState])
  extends RoutingState(parentState) with ContainerState

sealed abstract class FinalRoutingState(parentState: Option[ContainerRoutingState])
  extends RoutingState(parentState) with FinalState

case object RootState extends ContainerRoutingState(None)
case object SelectPageState extends FinalRoutingState(Some(RootState))
case object SettingsPageState extends FinalRoutingState(Some(RootState))
case class EditPageState(articleId: ArticleId) extends FinalRoutingState(Some(RootState))
