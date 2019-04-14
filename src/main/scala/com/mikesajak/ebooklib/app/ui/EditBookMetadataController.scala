package com.mikesajak.ebooklib.app.ui

import scalafx.scene.control.{Button, ComboBox, Spinner, TextField}
import scalafxml.core.macros.sfxml

@sfxml
class EditBookMetadataController(titleTextField: TextField,
                                 authorsCombo: ComboBox[String],
                                 seriesCombo: ComboBox[String],
                                 seriesNumSpinner: Spinner[Int],
                                 tagsCombo: ComboBox[String],
                                 identifiersTextField: TextField,
                                 creationDateTextField: TextField,
                                 creationDateSelButton: Button,
                                 publicationDateTextField: TextField,
                                 publicationDateSelButton: Button,
                                 publisherCombo: ComboBox[String],
                                 languagesCombo: ComboBox[String]) {

}
