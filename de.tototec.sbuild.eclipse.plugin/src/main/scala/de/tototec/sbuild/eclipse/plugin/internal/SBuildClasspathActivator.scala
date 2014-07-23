package de.tototec.sbuild.eclipse.plugin.internal

import org.eclipse.core.resources.IWorkspace
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.ILog
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Status
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import de.tototec.sbuild.eclipse.plugin.WorkspaceProjectChangeListener
import java.net.URLClassLoader
import de.tototec.sbuild.eclipse.plugin.Classpathes
import java.io.File
import org.eclipse.core.resources.IResourceChangeEvent
import org.sbuild.eclipse.resolver.SBuildResolver
import org.osgi.util.tracker.ServiceTracker
import org.osgi.framework.ServiceReference
import java.util.{ List => JList }
import org.sbuild.eclipse.resolver.{ Either => JEither }
import org.osgi.framework.Bundle
import scala.util.Failure
import scala.util.Try

/**
 * Companion object for bundle activator class [[SBuildClasspathActivator]].
 */
object SBuildClasspathActivator {
  /**
   * Access to the current activator instance.
   */
  def activator = _activator.getOrElse {
    throw new IllegalStateException("SBuild Eclipse Plugin not activated.");
  }
  private def activator_=(activator: SBuildClasspathActivator) = _activator = Option(activator)
  private[this] var _activator: Option[SBuildClasspathActivator] = None
}

/**
 * Bundle activator class for the SBuild Eclipse Plugin.
 */
class SBuildClasspathActivator extends BundleActivator {

  private[this] var _bundleContext: Option[BundleContext] = None
  def bundleContext = _bundleContext

  private[this] var onStop: List[BundleContext => Unit] = Nil

  @volatile private[this] var resolvers: Seq[SBuildResolver] = Seq()

  /**
   * Start of the bundle.
   */
  override def start(bundleContext: BundleContext) {
    SBuildClasspathActivator.activator = this;
    onStop ::= { _ => SBuildClasspathActivator.activator = null }

    this._bundleContext = Some(bundleContext)
    onStop ::= { _ => _bundleContext = None }

    de.tototec.sbuild.eclipse.plugin.debug("Starting bundle: " + bundleContext.getBundle)

    val tracker = new ServiceTracker(bundleContext, classOf[SBuildResolver].getName, null) {
      override def addingService(reference: ServiceReference): AnyRef = {
        de.tototec.sbuild.eclipse.plugin.info("Registering detected SBuild Resover: " + reference)
        val service = bundleContext.getService(reference).asInstanceOf[SBuildResolver]
        synchronized { resolvers ++= Seq(service) }
        service
      }
      override def removedService(reference: ServiceReference, service: AnyRef): Unit = {
        de.tototec.sbuild.eclipse.plugin.info("Unregistering SBuild Resover: " + reference)
        bundleContext.ungetService(reference)
        synchronized { resolvers = resolvers.filter(service.eq) }
      }
    }
    tracker.open();
    onStop ::= { _ => tracker.close() }

    // Register project change listener
    val workspaceProjectChangeListener = new WorkspaceProjectChangeListener()
    val workspace = ResourcesPlugin.getWorkspace
    workspace.addResourceChangeListener(workspaceProjectChangeListener, IResourceChangeEvent.POST_CHANGE)
    onStop ::= { _ => workspace.removeResourceChangeListener(workspaceProjectChangeListener) }

  }

  /** Stop of the bundle. */
  override def stop(bundleContext: BundleContext) = onStop.foreach { f => f(bundleContext) }

  def scanAndStartExtensionBundles(context: BundleContext): Unit = {
    val bundlesToStart = context.getBundles().filter { bundle =>
      bundle.getState() != Bundle.ACTIVE &&
        Option(bundle.getHeaders().get("SBuild-Service")).isDefined
    }
    bundlesToStart.foreach { bundle =>
      de.tototec.sbuild.eclipse.plugin.debug("About to manually start bundle: " + bundle)
      Try { bundle.start() } match {
        case Failure(e) =>
          de.tototec.sbuild.eclipse.plugin.error("Couldn't start bundle: " + bundle)
        case _ =>
      }
    }
  }

  def log: Option[ILog] = bundleContext.map(ctx => Platform.getLog(ctx.getBundle))

  def log(status: Int, msg: String, cause: Throwable = null) {
    log.map(_.log(new Status(status, bundleContext.get.getBundle.getSymbolicName, msg, cause)))
  }

  lazy val sbuildResolver: SBuildResolver = {
    bundleContext.map(scanAndStartExtensionBundles)

    new SBuildResolver {
      override def exportedDependencies(projectFile: File, exportName: String): JEither[Throwable, Array[String]] = {
        if (resolvers.isEmpty) {
          return JEither.left(new RuntimeException("No SBuild Resolvers found."))
        } else {
          val stream = resolvers.toStream.map(_.exportedDependencies(projectFile, exportName))
          stream.find(_.isRight()).getOrElse {
            JEither.left(new RuntimeException("No SBuild Resolver could successfully export. Causes:\n - " + stream.map(_.left().getLocalizedMessage()).mkString("\n - ")))
          }
        }
      }
      override def resolve(projectFile: File, dependency: String): JEither[Throwable, Array[File]] = {
        if (resolvers.isEmpty) {
          return JEither.left(new RuntimeException("No SBuild Resolvers found."))
        } else {
          val stream = resolvers.toStream.map(_.resolve(projectFile, dependency))
          stream.find(_.isRight()).getOrElse {
            JEither.left(new RuntimeException("No SBuild Resolver could successfully export. Causes:\n - " + stream.map(_.left().getLocalizedMessage()).mkString("\n - ")))
          }
        }
      }
    }
  }

}
