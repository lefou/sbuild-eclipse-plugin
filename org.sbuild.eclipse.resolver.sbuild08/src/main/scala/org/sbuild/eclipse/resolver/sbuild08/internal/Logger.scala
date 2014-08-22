package org.sbuild.eclipse.resolver.sbuild08.internal

import org.eclipse.core.runtime.IStatus
import org.osgi.framework.BundleContext
import org.eclipse.core.runtime.ILog
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status

object Logger {

  private[internal] var bundleContext: Option[BundleContext] = None

  private[this] def log(status: Int, msg: String, cause: Throwable = null): Unit = {
    bundleContext.foreach { ctx =>
      Platform.getLog(ctx.getBundle()).log(new Status(status, bundleContext.get.getBundle.getSymbolicName, msg, cause))
    }
  }

  /** Print a debug message. */
  private[sbuild08] def debug(msg: => String, cause: Throwable = null): Unit = {
    log(IStatus.INFO, msg, cause)
  }

  /** Print an info message. */
  private[sbuild08] def info(msg: => String, cause: Throwable = null): Unit = {
    log(IStatus.INFO, msg, cause)
  }

  /** Print an error message. */
  private[sbuild08] def error(msg: => String, cause: Throwable = null): Unit = {
    log(IStatus.ERROR, msg, cause)
  }

  /** Print a warn message. */
  private[sbuild08] def warn(msg: => String, cause: Throwable = null): Unit = {
    log(IStatus.WARNING, msg, cause)
  }

}