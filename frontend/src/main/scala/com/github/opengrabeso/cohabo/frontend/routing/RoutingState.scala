package com.github.opengrabeso.cohabo
package frontend
package routing

import dataModel._
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
  extends RoutingState(parentState) with State

sealed abstract class FinalRoutingState(parentState: Option[ContainerRoutingState])
  extends RoutingState(parentState) with State

case object RootState extends ContainerRoutingState(None)
case class SelectPageState(id: Option[ArticleIdModel]) extends FinalRoutingState(Some(RootState))
case object SettingsPageState extends FinalRoutingState(Some(RootState))
case object WorkflowsPageState extends FinalRoutingState(Some(RootState))
