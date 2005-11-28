package org.tigris.subversion.subclipse.ui.dialogs;

import java.io.File;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.history.ILogEntry;
import org.tigris.subversion.subclipse.ui.Policy;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class ExportRemoteFolderDialog extends Dialog {
	private ISVNRemoteFolder remoteFolder;
	private Text directoryText;
	private Text revisionText;
    private Button logButton;
    private Button headButton;
    private Button revisionButton;
	private Button okButton;
	private boolean success;

	public ExportRemoteFolderDialog(Shell parentShell, ISVNRemoteFolder remoteFolder) {
		super(parentShell);
		this.remoteFolder = remoteFolder;
	}
	
	protected Control createDialogArea(Composite parent) {
		getShell().setText(Policy.bind("ExportRemoteFolderAction.directoryDialogText")); //$NON-NLS-1$
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		
		Group repositoryGroup = new Group(composite, SWT.NULL);
		repositoryGroup.setText(Policy.bind("ExportRemoteFolderDialog.repository")); //$NON-NLS-1$
		GridLayout repositoryLayout = new GridLayout();
		repositoryLayout.numColumns = 2;
		repositoryGroup.setLayout(repositoryLayout);
		data = new GridData(GridData.FILL_BOTH);
		repositoryGroup.setLayoutData(data);
		
		Label urlLabel = new Label(repositoryGroup, SWT.NONE);
		urlLabel.setText(Policy.bind("ExportRemoteFolderDialog.url"));
		data = new GridData();
		data.horizontalSpan = 2;
		urlLabel.setLayoutData(data);
		
		Text urlText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		urlText.setLayoutData(data);
		urlText.setEditable(false);
		urlText.setText(remoteFolder.getUrl().toString());
		
		new Label(repositoryGroup, SWT.NONE);
		
		Label directoryLabel = new Label(repositoryGroup, SWT.NONE);
		directoryLabel.setText(Policy.bind("ExportRemoteFolderDialog.directory"));
		data = new GridData();
		data.horizontalSpan = 2;
		directoryLabel.setLayoutData(data);
		directoryText = new Text(repositoryGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 300;
		directoryText.setLayoutData(data);
		directoryText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setOkButtonStatus();		
			}			
		});
		
		Button directoryBrowseButton = new Button(repositoryGroup, SWT.PUSH);
		directoryBrowseButton.setText(Policy.bind("ExportRemoteFolderDialog.browse"));
		directoryBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.SAVE);
				dialog.setText(Policy.bind("ExportRemoteFolderAction.directoryDialogText"));
				String directory = dialog.open();
				if (directory != null) {
					directoryText.setText(directory);
					setOkButtonStatus();
				}
			}
		});
		
		Group revisionGroup = new Group(composite, SWT.NULL);
		revisionGroup.setText(Policy.bind("SwitchDialog.revision")); //$NON-NLS-1$
		GridLayout revisionLayout = new GridLayout();
		revisionLayout.numColumns = 3;
		revisionGroup.setLayout(revisionLayout);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		revisionGroup.setLayoutData(data);
		
		headButton = new Button(revisionGroup, SWT.RADIO);
		headButton.setText(Policy.bind("SwitchDialog.head")); //$NON-NLS-1$
		data = new GridData();
		data.horizontalSpan = 3;
		headButton.setLayoutData(data);
		
		revisionButton = new Button(revisionGroup, SWT.RADIO);
		revisionButton.setText(Policy.bind("SwitchDialog.revision")); //$NON-NLS-1$
		
		headButton.setSelection(true);
		
		revisionText = new Text(revisionGroup, SWT.BORDER);
		data = new GridData();
		data.widthHint = 40;
		revisionText.setLayoutData(data);
		revisionText.setEnabled(false);
		
		revisionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setOkButtonStatus();
            }		    
		});
		
		logButton = new Button(revisionGroup, SWT.PUSH);
		logButton.setText(Policy.bind("MergeDialog.showLog")); //$NON-NLS-1$
		logButton.setEnabled(false);
		logButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showLog();
            }
		});	
		
		SelectionListener listener = new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                revisionText.setEnabled(revisionButton.getSelection());
                logButton.setEnabled(revisionButton.getSelection());
                setOkButtonStatus();
                if (revisionButton.getSelection()) {
                    revisionText.selectAll();
                    revisionText.setFocus();
                }
            }
		};
		
		headButton.addSelectionListener(listener);
		revisionButton.addSelectionListener(listener);
		
		directoryText.setFocus();
		
		return composite;
	}
	
    protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
        Button button = super.createButton(parent, id, label, defaultButton);
		if (id == IDialogConstants.OK_ID) {
			okButton = button; 
			okButton.setEnabled(false);
		}
        return button;
    }
    
    private void setOkButtonStatus() {
        okButton.setEnabled((directoryText.getText().trim().length() > 0) && (headButton.getSelection() || (revisionText.getText().trim().length() > 0)));
    }
    
	protected void showLog() {
        HistoryDialog dialog = dialog = new HistoryDialog(getShell(), remoteFolder);
        if (dialog.open() == HistoryDialog.CANCEL) return;
        ILogEntry[] selectedEntries = dialog.getSelectedLogEntries();
        if (selectedEntries.length == 0) return;
        revisionText.setText(Long.toString(selectedEntries[selectedEntries.length - 1].getRevision().getNumber()));
        setOkButtonStatus();
    }

	protected void okPressed() {
		success = true;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					ISVNClientAdapter client = SVNProviderPlugin.getPlugin().getSVNClientManager().createSVNClient();
					SVNRevision revision = null;
					if (headButton.getSelection()) revision = SVNRevision.HEAD;
					else {
						int revisionNumber = Integer.parseInt(revisionText.getText().trim());
						long revisionLong = revisionNumber;
						revision = new SVNRevision.Number(revisionLong);
					}
					File directory = new File(directoryText.getText().trim());
					client.doExport(remoteFolder.getUrl(), directory, revision, true);		
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Policy.bind("ExportRemoteFolderAction.directoryDialogText"), e.getMessage()); //$NON-NLS-1$
					success = false;
				}
			}			
		});
		if (!success) return;
		super.okPressed();
	}

}