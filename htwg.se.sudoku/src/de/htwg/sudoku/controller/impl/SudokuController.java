package de.htwg.sudoku.controller.impl;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.undo.UndoManager;

import de.htwg.sudoku.model.ICell;
import de.htwg.sudoku.model.IGrid;
import de.htwg.sudoku.controller.ISudokuController;
import de.htwg.sudoku.controller.ISudokuControllerGui;
import de.htwg.util.observer.Observable;

public class SudokuController extends Observable implements ISudokuController, ISudokuControllerGui {
	
	private String statusLine = "Welcome to HTWG Sudoku!";
	private IGrid grid;
	private UndoManager undoManager;
	private int highlighted=0;
	
	public SudokuController(IGrid grid) {
		this.grid = grid;
		this.undoManager = new UndoManager();
	}
	
	public void setValue(int row, int column, int value) {
		ICell cell = grid.getICell(row, column);
		if (cell.isUnSet()) {
			cell.setValue(value);
			undoManager.addEdit(new SetValueCommand(cell));
			statusLine = "The cell " + cell.mkString() + " was successfully set";
		} else {
			statusLine="The cell " + cell.mkString() + " is already set";
		}
		notifyObservers();
	}
	
	public void solve() {
		boolean result;
		result = grid.solve();		
		if (result) {
			statusLine="The Sudoku was solved successfully";
		} else {
			statusLine="Can not solve this Sudoku within "
					+ grid.getSteps() + " steps";
		}
		notifyObservers();
	}
	public void reset() {
		grid.reset();
		statusLine = "Sudoku was reset";
		notifyObservers();
	}
	
	public void create() {
		grid.create();
		highlighted=0;
		statusLine= "New Sudoku Puzzle created";
		notifyObservers();
	}

	public String getStatus() {
		return statusLine;
	}

	public String getGridString() {
		return grid.toString();
	}
	
	public void undo() {
		if (undoManager.canUndo()){
			undoManager.undo();
		}
		statusLine = "Undo";
		notifyObservers();
	}

	public void redo() {
		if (undoManager.canRedo()){
			undoManager.redo();
		}
		statusLine= "Redo";
		notifyObservers();
	}
	
	public void copy() {
		StringSelection gridString = new StringSelection(grid.toString("0"));
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				gridString, null);
		statusLine= "Copied Sudoku";
		notifyObservers();
	}

	public void paste() {
		Transferable transferable = Toolkit.getDefaultToolkit()
				.getSystemClipboard().getContents(null);
		if (transferable != null
				&& transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			String input;
			try {
				input = (String) transferable
						.getTransferData(DataFlavor.stringFlavor);
				grid.parseStringToGrid(input);
			} catch (UnsupportedFlavorException e1) {

				e1.printStackTrace();
			} catch (IOException e1) {

				e1.printStackTrace();
			}
		}
		statusLine= "Pasted Sudoku";
		notifyObservers();
	}

	public int getValue(int row, int column) {
		return grid.getICell(row, column).getValue();
	}

	//@Override
	public void reset(IGrid grid) {
		this.grid = grid;
		reset();
		notifyObservers();
	}

	public void showCandidates(int row, int column) {
		grid.getICell(row, column).toggleShowCandidates();
		BitSet set = grid.candidates(row,column);
		statusLine = "Candidates at ("+row+","+column+") are "+set.toString();
		notifyObservers();
	}

	public void highlight(int value) {
		highlighted=value;
		notifyObservers();
	}

	public int getCellsPerEdge() {
		return grid.getCellsPerEdge();
	}

	public int getBlockSize() {
		return grid.getBlockSize();
	}

	public int blockAt(int row, int column) {
		return grid.blockAt(row, column);
	}

	public void exit() {
		System.exit(0);
	}

	public void showAllCandidates() {
		for (int row = 0; row < grid.getCellsPerEdge(); row++) {
			for (int col = 0; col < grid.getCellsPerEdge(); col++) {
				showCandidates(row, col);
			}	
		}
		notifyObservers();
	}

	public boolean isGiven(int row, int column) {
		return grid.getICell(row, column).isGiven();
	}

	public boolean isHighlighted(int row, int column) {
		return grid.candidates(row, column).get(highlighted);
	}

	public boolean isSet(int row, int column) {
		return grid.getICell(row, column).isSet();
	}

	public boolean isShowCandidates(int row, int column) {
		return grid.getICell(row, column).isShowCandidates();
	}

	public boolean isCandidate(int row, int column, int candidate) {
		return grid.candidates(row, column).get(candidate);
	}
	
	public void load(JFrame frame){
		JFileChooser fileChooser = new JFileChooser(".");
		int result = fileChooser.showOpenDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION) {
			try {
				FileInputStream fis = new FileInputStream(fileChooser
						.getSelectedFile());
				ObjectInputStream inStream = new ObjectInputStream(fis);
				grid.parseStringToGrid((String) inStream.readObject());
				inStream.close();
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(frame,
						"IOException reading sudoku:\n"
								+ ioe.getLocalizedMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			} catch (ClassNotFoundException e) {
				statusLine="Can not load: Class not found";
			}
		}
		notifyObservers();
	}

	public void save(JFrame frame) {
		JFileChooser fileChooser = new JFileChooser(".");
		int result = fileChooser.showSaveDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (file.exists()
					&& JOptionPane.showConfirmDialog(frame, "File \""
							+ file.getName() + "\" already exists.\n"
							+ "Would you like to replace it?", "Save",
							JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				return;
			}
			try {
				FileOutputStream fos = new FileOutputStream(file);
				ObjectOutputStream outStream = new ObjectOutputStream(fos);
				outStream.writeObject(grid.toString());
				outStream.flush();
				outStream.close();

			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(frame,
						"IOException saving sudoku:\n"
								+ ioe.getLocalizedMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
		notifyObservers();
	}

}