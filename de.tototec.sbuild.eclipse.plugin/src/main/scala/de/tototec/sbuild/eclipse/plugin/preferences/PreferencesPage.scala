package de.tototec.sbuild.eclipse.plugin.preferences

import org.eclipse.jface.preference.FieldEditorPreferencePage
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage

import de.tototec.sbuild.eclipse.plugin.internal.SBuildClasspathActivator

class PreferencesPage()
    extends FieldEditorPreferencePage(FieldEditorPreferencePage.GRID)
    with IWorkbenchPreferencePage {

  override def init(workbench: IWorkbench): Unit = {
    val prefsStore = SBuildClasspathActivator.activator.getPreferenceStore()
    setPreferenceStore(prefsStore)
    setDescription("SBuild configuration")
  }

  override protected def createFieldEditors(): Unit = {}

}