/*
 * Copyright (c) Thomas Parker, 2013-14.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package pcgen.gui2.solverview;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import pcgen.base.formula.base.LegalScope;
import pcgen.base.formula.base.ScopeInstance;
import pcgen.base.formula.base.VarScoped;
import pcgen.base.formula.base.VariableID;
import pcgen.base.solver.ProcessStep;
import pcgen.base.solver.SplitFormulaSetup;
import pcgen.cdom.enumeration.CharID;
import pcgen.cdom.facet.FacetLibrary;
import pcgen.cdom.facet.FormulaSetupFacet;
import pcgen.cdom.facet.ScopeFacet;
import pcgen.cdom.facet.SolverManagerFacet;
import pcgen.cdom.facet.VariableLibraryFacet;
import pcgen.facade.core.CharacterFacade;
import pcgen.gui2.tools.Utility;
import pcgen.gui2.util.JComboBoxEx;
import pcgen.system.CharacterManager;
import pcgen.system.LanguageBundle;

public class SolverViewFrame extends JFrame
{

	private static final ScopeFacet scopeFacet = FacetLibrary
		.getFacet(ScopeFacet.class);
	private static final VariableLibraryFacet variableLibraryFacet =
			FacetLibrary.getFacet(VariableLibraryFacet.class);
	private static final SolverManagerFacet solverManagerFacet = FacetLibrary
		.getFacet(SolverManagerFacet.class);
	private static final FormulaSetupFacet formulaSetupFacet = FacetLibrary
		.getFacet(FormulaSetupFacet.class);

	private JComboBoxEx scopeChooser;
	private LegalScope selectedScope;

	private JTextField varName;
	private String varNameText = "                               ";

	private JComboBoxEx objectChooser;
	private VarScoped activeObject;

	private JComboBoxEx identifierChooser;
	private CharID activeIdentifier;

	private JTable viewTable;

	private SolverTableModel tableModel;

	public SolverViewFrame(Frame frame)
	{
		identifierChooser = new JComboBoxEx();
		for (CharacterFacade pcf : CharacterManager.getCharacters())
		{
			String pcname = pcf.getNameRef().getReference();
			CharID id = pcf.getCharID();
			identifierChooser.addItem(new PCRef(pcname, id));
		}
		identifierChooser.addActionListener(new IdentifierActionListener());

		objectChooser = new JComboBoxEx();
		objectChooser.addActionListener(new ObjectActionListener());

		scopeChooser = new JComboBoxEx();
		scopeChooser.addActionListener(new ScopeActionListener());

		identifierChooser.setSelectedItem(identifierChooser.getItemAt(0));
		scopeChooser.setSelectedItem(scopeChooser.getItemAt(0));

		varName = new JTextField();
		varName.setText(varNameText);
		varName.getDocument().addDocumentListener(new VarNameListener());

		initialize();
		objectChooser.setSelectedItem(objectChooser.getItemAt(0));
	}

	private void update()
	{
		updateObjects();
		ScopeInstance scope =
				scopeFacet.get(activeIdentifier, selectedScope, activeObject);
		if (variableLibraryFacet
			.isLegalVariableName(activeIdentifier.getDatasetID(),
				scope.getLegalScope(), varNameText))
		{
			displayInfo(scope);
		}
		else
		{
			//TODO Update a status bar
			System.err.println(selectedScope.getName()
				+ " does not have a variable: " + varNameText);
		}
	}

	private void displayInfo(ScopeInstance scope)
	{
		VariableID<?> varID =
				variableLibraryFacet.getVariableID(
					activeIdentifier.getDatasetID(), scope, varNameText);
		setSteps(varID);
	}

	private <T> void setSteps(VariableID<T> varID)
	{
		List<ProcessStep<T>> steps =
				solverManagerFacet.diagnose(activeIdentifier, varID);
		tableModel.setSteps(steps);
	}

	private class ScopeActionListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			LegalScopeWrapper wrap =
					(LegalScopeWrapper) scopeChooser.getSelectedItem();
			selectedScope = wrap.getLegalScope();
			update();
		}

	}

	private class ObjectActionListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			ObjectNameDisplayer displayer =
					(ObjectNameDisplayer) objectChooser.getSelectedItem();
			if (displayer == null)
			{
				activeObject = null;
				tableModel.setSteps(Collections.emptyList());
			}
			else
			{
				activeObject = displayer.getObject();
				update();
			}
		}

	}

	private class VarNameListener implements DocumentListener
	{

		@Override
		public void insertUpdate(DocumentEvent e)
		{
			varNameText = varName.getText().trim();
			update();
		}

		@Override
		public void removeUpdate(DocumentEvent e)
		{
			varNameText = varName.getText().trim();
			update();
		}

		@Override
		public void changedUpdate(DocumentEvent e)
		{
			varNameText = varName.getText().trim();
			update();
		}

	}

	private class IdentifierActionListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e)
		{
			Object item = identifierChooser.getSelectedItem();
			activeIdentifier = ((PCRef) item).id;
			SplitFormulaSetup formulaSetup =
					formulaSetupFacet.get(activeIdentifier.getDatasetID());
			for (LegalScope lvs : formulaSetup.getLegalScopeLibrary().getLegalScopes())
			{
				scopeChooser.addItem(new LegalScopeWrapper(lvs));
			}
			update();
		}

	}

	private void updateObjects()
	{
		if (activeIdentifier != null)
		{
			Collection<VarScoped> objects =
					scopeFacet.getObjectsWithVariables(activeIdentifier);
			objectChooser.removeAllItems();
			String scopeName = selectedScope.getName();
			for (VarScoped cdo : objects)
			{
				if (cdo.getLocalScopeName().equals(scopeName))
				{
					if (scopeFacet.get(activeIdentifier, selectedScope, cdo) != null)
					{
						objectChooser.addItem(new ObjectNameDisplayer(cdo));
					}
				}
			}
			if (objectChooser.getItemCount() != 0)
			{
				objectChooser.setSelectedIndex(0);
			}
		}
	}

	public void initialize()
	{
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		getContentPane().setLayout(gridbag);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(2, 2, 2, 2);

		int col = 0;
		Utility.buildConstraints(c, col, 0, 1, 1, 100, 20);
		JLabel label =
				new JLabel(
					LanguageBundle
						.getFormattedString("in_SolverView_Perspective")); //$NON-NLS-1$
		gridbag.setConstraints(label, c);
		getContentPane().add(label);

		Utility.buildConstraints(c, col++, 1, 1, 1, 0, 20);
		gridbag.setConstraints(identifierChooser, c);
		getContentPane().add(identifierChooser);

		Utility.buildConstraints(c, col++, 1, 1, 1, 0, 20);
		gridbag.setConstraints(scopeChooser, c);
		getContentPane().add(scopeChooser);

		Utility.buildConstraints(c, col++, 1, 1, 1, 0, 20);
		gridbag.setConstraints(objectChooser, c);
		getContentPane().add(objectChooser);

		Utility.buildConstraints(c, col++, 1, 1, 1, 0, 20);
		label =
				new JLabel(
					LanguageBundle.getFormattedString("in_SolverView_VarName") //$NON-NLS-1$
				);
		gridbag.setConstraints(label, c);
		getContentPane().add(label);

		Utility.buildConstraints(c, col++, 1, 1, 1, 0, 20);
		gridbag.setConstraints(varName, c);
		getContentPane().add(varName);

		tableModel = new SolverTableModel();
		viewTable = new JTable(tableModel);

		viewTable.getColumnModel().getColumn(0).setPreferredWidth(25);
		viewTable.getColumnModel().getColumn(1).setPreferredWidth(50);
		viewTable.getColumnModel().getColumn(2).setPreferredWidth(25);
		viewTable.getColumnModel().getColumn(3).setPreferredWidth(50);

		Utility.buildConstraints(c, 0, 2, col, 1, 0, 1000);
		JScrollPane pane = new JScrollPane(viewTable);
		viewTable.setFillsViewportHeight(true);
		pane.setPreferredSize(new Dimension(500, 300));
		gridbag.setConstraints(pane, c);
		getContentPane().add(pane);

		setTitle("Core Variable Debug View");
		getContentPane().setSize(500, 400);
		pack();
		Utility.centerFrame(this, true);
	}

	private static class SolverTableModel<T> extends AbstractTableModel
	{
		private String[] columnNames = {"Modification Type", "Modification",
			"Resulting Value", "Priority", "Source"};

		private List<ProcessStep<T>> steps = Collections.emptyList();

		@Override
		public String getColumnName(int column)
		{
			return columnNames[column];
		}

		@Override
		public int getRowCount()
		{
			return steps.size();
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			ProcessStep<T> ps = steps.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return ps.getModifier().getIdentification();
				case 1:
					return ps.getModifier().getInstructions();
				case 2:
					return ps.getResult();
				case 3:
					return ps.getModifier().getUserPriority();
				case 4:
					return ps.getSourceInfo();
				default:
					return "";
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}

		public void setSteps(List<ProcessStep<T>> steps)
		{
			this.steps = steps;
			fireTableDataChanged();
		}
	}

	private class PCRef
	{
		public String name;
		public CharID id;

		public PCRef(String pcname, CharID id)
		{
			this.name = pcname;
			this.id = id;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}