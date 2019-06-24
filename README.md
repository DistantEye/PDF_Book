# PDF_Book

Configurable tabbed PDF viewer meant for managing a stack of open reference materials at once. Speed and memory use is not competitive with traditional PDF readers, but this isn't the focus. The entire user experience is tailored towards organization and navigation between already known pages, becoming the virtual equivalent of a stack of books open at your desk:

* Open the same PDF multiple times into separate tabs

* Tabs display prominently and readably on the left

* Tab names can be renamed to whatever you like

* Everything that can be remembered between tab switches is:
	* Page number
	* Zoom size
	* Exact position of scrollbar
	
* Open tabs and all settings (including the above) can be saved to file and reloaded later

* Numerous keyboard shortcuts for quick flipping

## User Install Notes:

PDF_Book ships with an integrated PDF Renderer (via Apache PDF Box). This is however, for many use cases, insufficient : it uses up an incredible amount of memory and runs very slowly on complex PDFs.

As a preferred alternative, the program will attempt to look for the presence of MuPDF's commandline tool on startup, and switch to using that if possible. Obtain the appropriate mutool executable from your OS's appropriate package manager (or https://mupdf.com/ ) and copy or symbolic link mutool to ./external/mutool in the project directory.

Since mutool is the preferred renderer, the UI will display a warning message in the menu bar if it was unable to find the external renderer. It'll always be non-ambiguous whether PDF_Book managed to successfully connect to the external executable or not.

## Dev Install Notes

The eclipse project file is committed as part of the repo, so you should be able to import from there, or import ./pom.xml /src and /external into your dev environment of choice.

Maven should download all depedencies cleanly as needed. Note the User Install Notes for how to setup MuPDF's renderer

## Keyboard Shortcuts

Ctrl + [1-9] will switch to the first through ninth tab.

Ctrl + [Up/Down] will switch one up or one down.

Ctrl + [Left/Right] will change the current page on the selected tab.

Ctrl + [-/+] will change the DPI (image size) on the selected tab.

Ctrl + c will clone the current tab and switch to the clone.

Ctrl + r will bring up the rename prompt

Ctrl + g will focus the page num text field and clear it, so a new page can be entered.

Ctrl + o will bring up a prompt to change the current tab's position, moving it higher/lower in the list.

Tip: The PageNum and DPI textboxes may block shortcuts when active. Click outside of them to break focus

## Limitations

* Memory consumption for the integrated PDF_Box Renderer is through the roof and there's nothing that can be done about that

* Memory consumption for the MuPDF renderer is better but can still spike from the JVM opting not to clean up after itself for a while. It does eventually go down.

* Image output only. Selectable text is really complicated. The *most* one could probably do is add a button/keyboard shortcut to retrieve unformatted text data for the current page. Integrating it with the image display is right out

## Liscense Notes

MuPDF's inclusion in the project would generally mandate this project and any derivatives of it fall under either AGPLv3 or GPLv3. It's not entirely clear whether the current model of not including MuPDF binaries directly but prompting users to load them into the project during install counts, but to remove any ambiguity, PDF_Book is liscensed under GPLv3 anyways, so all bases should be covered.