# PDF-Reader
An app on Android tablet, like GoodNotes on iPad.

Back button: switch to previous page
Forward button: switch to next page
Left arrow: undo
Right arrow: redo
Pen button: draw
Brush button: highlight
Cross button: erase (by tapping)

Each of last three buttons has two states, selected and unselected. They act as toggle buttons.
Select any one of these will unselect the rest of the two.
If none of these three are selected, it is in gesture mode.
All supported gestures are handled in gesture mode.
Panning is available only if page has been zoomed.

Additional supported gesture:
Double tap: reset the page to unscaled, untranslated.

Currently missing the functionality to save data when terminate and restart the app.