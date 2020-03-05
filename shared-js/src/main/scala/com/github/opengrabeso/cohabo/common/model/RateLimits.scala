package com.github.opengrabeso.cohabo
package common.model

import rest.EnhancedRestDataCompanion

// https://developer.github.com/v3/rate_limit/

case class RateLimit(
  limit: Long,
  remaining: Long,
  reset: Long // timestamp
)

object RateLimit extends EnhancedRestDataCompanion[RateLimit]

case class RateLimitResources(
  core: RateLimit,
  search: RateLimit,
  graphql: RateLimit,
  integration_manifest: RateLimit
)

object RateLimitResources extends EnhancedRestDataCompanion[RateLimitResources]

case class RateLimits(resources: RateLimitResources)

object RateLimits extends EnhancedRestDataCompanion[RateLimits]
