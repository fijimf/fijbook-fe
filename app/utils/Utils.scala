package utils

import play.api.mvc.RequestHeader

object Utils {
  def isAdminRequest(request: RequestHeader): Boolean = {
    request.path.startsWith("/deepfij/admin")
  }
}
