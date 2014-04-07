package ai.aitia.petplugin.wizards.dialogfields;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class ButtonDialogField extends StringButtonDialogField {
	
	public ButtonDialogField(IStringButtonAdapter adapter) {
		super(adapter);
	}
	
	@Override
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {

		assertEnoughColumns(nColumns);
		Text text= getTextControl(parent);
		text.setLayoutData(gridDataForText(nColumns - 1));
		Button button= getChangeControl(parent);
		button.setLayoutData(gridDataForButton(button, 1));

		return new Control[] {text, button };
	}
	
	@Override
	public int getNumberOfControls() {
		return 2;
	}

}
