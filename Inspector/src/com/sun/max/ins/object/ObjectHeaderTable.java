/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.object;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.debug.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.memory.*;
import com.sun.max.ins.type.*;
import com.sun.max.ins.value.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * A table that displays the header in a Maxine heap object; for use in an instance of {@link ObjectInspector}.
 *
 * @author Michael Van De Vanter
 */
public final class ObjectHeaderTable extends InspectorTable {

    private final TeleObject teleObject;
    private final IndexedSequence<Layout.HeaderField> headerFields;

    private final ObjectHeaderTableModel tableModel;
    private final ObjectHeaderColumnModel columnModel;

    private final class ToggleObjectHeaderWatchpointAction extends InspectorAction {

        private final int row;

        public ToggleObjectHeaderWatchpointAction(Inspection inspection, String name, int row) {
            super(inspection, name);
            this.row = row;
        }

        @Override
        protected void procedure() {
            final Sequence<MaxWatchpoint> watchpoints = tableModel.getWatchpoints(row);
            if (watchpoints.isEmpty()) {
                final HeaderField headerField = headerFields.get(row);
                actions().setHeaderWatchpoint(teleObject, headerField, "Watch this field's memory").perform();
            } else {
                actions().removeWatchpoints(watchpoints, null).perform();
            }
        }
    }

    private final ObjectViewPreferences instanceViewPreferences;

    /**
     * A {@link JTable} specialized to display Maxine object header fields.
     *
     * @param objectInspector parent that contains this panel
     */
    public ObjectHeaderTable(Inspection inspection, TeleObject teleObject, ObjectViewPreferences instanceViewPreferences) {
        super(inspection);
        this.teleObject = teleObject;
        this.instanceViewPreferences = instanceViewPreferences;
        headerFields = new ArrayListSequence<Layout.HeaderField>(teleObject.getHeaderFields());
        this.tableModel = new ObjectHeaderTableModel(inspection, teleObject.getCurrentOrigin());
        this.columnModel = new ObjectHeaderColumnModel(instanceViewPreferences);
        configureMemoryTable(tableModel, columnModel);
        setBorder(BorderFactory.createMatteBorder(3, 0, 0, 0, inspection.style().defaultBorderColor()));
        updateFocusSelection();
    }

    @Override
    protected void mouseButton1Clicked(int row, int col, MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1 && maxVM().watchpointsEnabled()) {
            final InspectorAction action = new ToggleObjectHeaderWatchpointAction(inspection(), null, row);
            action.perform();
        }
    }

    @Override
    protected InspectorPopupMenu getPopupMenu(int row, int col, MouseEvent mouseEvent) {
        if (maxVM().watchpointsEnabled()) {
            final InspectorPopupMenu menu = new InspectorPopupMenu();
            menu.add(new ToggleObjectHeaderWatchpointAction(inspection(), "Toggle watchpoint (double-click)", row));
            final HeaderField headerField = headerFields.get(row);
            menu.add(actions().setHeaderWatchpoint(teleObject, headerField, "Watch this field's memory"));
            menu.add(actions().setObjectWatchpoint(teleObject, "Watch this object's memory"));
            menu.add(Watchpoints.createEditMenu(inspection(), tableModel.getWatchpoints(row)));
            menu.add(Watchpoints.createRemoveActionOrMenu(inspection(), tableModel.getWatchpoints(row)));
            return menu;
        }
        return null;
    }

    @Override
    public void updateFocusSelection() {
        // Sets table selection to the memory word, if any, that is the current user focus.
        final Address address = inspection().focus().address();
        updateSelection(tableModel.findRow(address));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // The selection in the table has changed; might have happened via user action (click, arrow) or
        // as a side effect of a focus change.
        super.valueChanged(e);
        if (!e.getValueIsAdjusting()) {
            final int row = getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount()) {
                inspection().focus().setAddress(tableModel.getAddress(row));
            }
        }
    }

    /**
     * A column model for object headers, to be used in an {@link ObjectInspector}. Column selection is driven by
     * choices in the parent {@link ObjectInspector}. This implementation cannot update column choices dynamically.
     */
    private final class ObjectHeaderColumnModel extends InspectorTableColumnModel<ObjectColumnKind> {

        ObjectHeaderColumnModel(ObjectViewPreferences viewPreferences) {
            super(ObjectColumnKind.VALUES.length(), viewPreferences);
            addColumn(ObjectColumnKind.TAG, new TagRenderer(inspection()), null);
            addColumn(ObjectColumnKind.ADDRESS, new AddressRenderer(inspection()), null);
            addColumn(ObjectColumnKind.OFFSET, new PositionRenderer(inspection()), null);
            addColumn(ObjectColumnKind.TYPE, new TypeRenderer(inspection()), null);
            addColumn(ObjectColumnKind.NAME, new NameRenderer(inspection()), null);
            addColumn(ObjectColumnKind.VALUE, new ValueRenderer(inspection()), null);
            addColumn(ObjectColumnKind.REGION, new RegionRenderer(inspection()), null);
        }
    }

    /**
     * Models the words/rows in an object header; the value of each cell is simply the word/row number.
     * <br>
     * The origin of the model is the current origin of the object in memory, which can change due to GC.
     */
    private final class ObjectHeaderTableModel extends InspectorMemoryTableModel {

        private TeleHub teleHub;

        public ObjectHeaderTableModel(Inspection inspection, Address origin) {
            super(inspection, origin);
            if (teleObject.isLive()) {
                teleHub = teleObject.getTeleHub();
            }
        }

        public int getColumnCount() {
            return ObjectColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return teleObject.getHeaderFields().size();
        }

        public Object getValueAt(int row, int col) {
            return row;
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return Integer.class;
        }

        @Override
        public Address getAddress(int row) {
            return getOrigin().plus(getOffset(row));
        }

        @Override
        public MemoryRegion getMemoryRegion(int row) {
            return teleObject.getCurrentMemoryRegion(headerFields.get(row));
        }

        @Override
        public Offset getOffset(int row) {
            return teleObject.getHeaderOffset(headerFields.get(row));
        }

        @Override
        public int findRow(Address address) {
            for (int row = 0; row < headerFields.length(); row++) {
                if (getAddress(row).equals(address)) {
                    return row;
                }
            }
            return -1;
        }

        public TypeDescriptor rowToType(int row) {
            return teleObject.getHeaderType(headerFields.get(row));
        }

        public String rowToName(int row) {
            return headerFields.get(row).toString();
        }

        public TeleHub teleHub() {
            return teleHub;
        }

        @Override
        public void refresh() {
            setOrigin(teleObject.getCurrentOrigin());
            if (teleObject.isLive()) {
                teleHub = teleObject.getTeleHub();
            }
            super.refresh();
        }
    }

    /**
     * @return color the text specially in the row where a watchpoint is triggered
     */
    private Color getRowTextColor(int row) {
        final MaxWatchpointEvent watchpointEvent = maxVMState().watchpointEvent();
        if (watchpointEvent != null && tableModel.getMemoryRegion(row).contains(watchpointEvent.address())) {
            return style().debugIPTagColor();
        }
        return style().defaultTextColor();
    }

    private final class TagRenderer extends MemoryTagTableCellRenderer implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            final Component renderer = getRenderer(tableModel.getMemoryRegion(row), focus().thread(), tableModel.getWatchpoints(row));
            renderer.setForeground(getRowTextColor(row));
            renderer.setBackground(cellBackgroundColor(isSelected));
            return renderer;
        }
    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithOffset implements TableCellRenderer {

        AddressRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsOffset implements TableCellRenderer {

        public PositionRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.getOffset(row), tableModel.getOrigin());
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class TypeRenderer extends TypeLabel implements TableCellRenderer {

        public TypeRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setValue(tableModel.rowToType(row));
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class NameRenderer extends JavaNameLabel implements TableCellRenderer {

        public NameRenderer(Inspection inspection) {
            super(inspection);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(tableModel.rowToName(row));
            setForeground(getRowTextColor(row));
            setBackground(cellBackgroundColor(isSelected));
            return this;
        }
    }

    private final class ValueRenderer implements TableCellRenderer, Prober {

        private InspectorLabel[] labels = new InspectorLabel[headerFields.length()];

        public ValueRenderer(Inspection inspection) {

            for (int row = 0; row < headerFields.length(); row++) {
                // Create a label suitable for the kind of header field
                InspectorLabel label = null;
                switch(headerFields.get(row)) {
                    case HUB:
                        label = new WordValueLabel(inspection, WordValueLabel.ValueMode.REFERENCE, ObjectHeaderTable.this) {

                            @Override
                            public Value fetchValue() {
                                final TeleHub teleHub = tableModel.teleHub();
                                return teleHub == null ? WordValue.ZERO : WordValue.from(teleHub.getCurrentOrigin());
                            }
                        };
                        break;
                    case MISC:
                        label = new MiscWordLabel(inspection, teleObject);
                        break;
                    case LENGTH:
                        switch (teleObject.getObjectKind()) {
                            case ARRAY:
                                final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject;
                                label = new PrimitiveValueLabel(inspection, Kind.INT) {

                                    @Override
                                    public Value fetchValue() {
                                        return IntValue.from(teleArrayObject.getLength());
                                    }
                                };
                                break;
                            case HYBRID:
                                final TeleHybridObject teleHybridObject = (TeleHybridObject) teleObject;
                                label = new PrimitiveValueLabel(inspection, Kind.INT) {

                                    @Override
                                    public Value fetchValue() {
                                        return IntValue.from(teleHybridObject.readArrayLength());
                                    }
                                };
                                break;
                            case TUPLE:
                                // No length header field
                                break;
                            default:
                                ProgramError.unknownCase();
                        }
                        break;
                    default:
                        ProgramError.unknownCase();
                }
                label.setOpaque(true);
                labels[row] = label;
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final InspectorLabel inspectorLabel = labels[row];
            inspectorLabel.setBackground(cellBackgroundColor(isSelected));
            return inspectorLabel;
        }

        public void redisplay() {
            for (InspectorLabel label : labels) {
                label.redisplay();
            }
        }

        public void refresh(boolean force) {
            for (InspectorLabel label : labels) {
                label.refresh(force);
            }
        }
    }

    private final class RegionRenderer implements TableCellRenderer, Prober {

        private final InspectorLabel regionLabel;
        private final InspectorLabel dummyLabel;

        public RegionRenderer(Inspection inspection) {
            regionLabel = new MemoryRegionValueLabel(inspection) {

                @Override
                public Value fetchValue() {
                    final TeleHub teleHub = tableModel.teleHub();
                    if (teleHub != null) {
                        return WordValue.from(teleHub.getCurrentOrigin());
                    }
                    return WordValue.ZERO;
                }
            };
            regionLabel.setOpaque(true);
            dummyLabel = new PlainLabel(inspection, "");
            dummyLabel.setOpaque(true);
        }

        public void refresh(boolean force) {
            regionLabel.refresh(force);
        }

        public void redisplay() {
            regionLabel.redisplay();
            dummyLabel.redisplay();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final InspectorLabel inspectorLabel =  (headerFields.get(row) == HeaderField.HUB) ? regionLabel : dummyLabel;
            inspectorLabel.setBackground(cellBackgroundColor(isSelected));
            return inspectorLabel;
        }
    }

}
