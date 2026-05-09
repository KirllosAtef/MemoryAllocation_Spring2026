package util;

import javafx.application.Platform;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

public class EditCell<S, T> extends TableCell<S, T> {

    private TextField textField;
    private final StringConverter<T> converter;

    public EditCell(StringConverter<T> converter) {
        this.converter = converter;
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
        return list -> new EditCell<>(new DefaultStringConverter());
    }

    @Override
    public void startEdit() {
        if (!isEmpty() && isEditable() && getTableView().isEditable() && getTableColumn().isEditable()) {
            super.startEdit();
            if (textField == null) {
                createTextField();
            }
            textField.setText(getItemText());
            setText(null);
            setGraphic(textField);
            
            // Need runLater to successfully select text
            Platform.runLater(() -> {
                textField.requestFocus();
                textField.selectAll();
            });
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setText(getItemText());
        setGraphic(null);
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getItemText());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getItemText());
                setGraphic(null);
            }
        }
    }

    private void createTextField() {
        textField = new TextField(getItemText());
        
        // Commit on focus loss
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && isEditing()) {
                commitEdit(converter.fromString(textField.getText()));
            }
        });

        // Handle Enter and Escape
        textField.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.ENTER) {
                commitEdit(converter.fromString(textField.getText()));
                
                // Move focus to next adjacent cell
                Platform.runLater(() -> {
                    TableView<S> table = getTableView();
                    TableColumn<S, ?> col = getTableColumn();
                    int colIndex = table.getColumns().indexOf(col);
                    int rowIndex = getTableRow().getIndex();
                    
                    if (colIndex < table.getColumns().size() - 1) {
                        // Move to next column in same row
                        TableColumn<S, ?> nextCol = table.getColumns().get(colIndex + 1);
                        table.getSelectionModel().clearAndSelect(rowIndex, nextCol);
                        table.edit(rowIndex, nextCol);
                    } else if (rowIndex < table.getItems().size() - 1) {
                        // Move to first column of next row
                        TableColumn<S, ?> nextCol = table.getColumns().get(0);
                        table.getSelectionModel().clearAndSelect(rowIndex + 1, nextCol);
                        table.edit(rowIndex + 1, nextCol);
                    }
                });
            } else if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    private String getItemText() {
        return getItem() == null ? "" : converter.toString(getItem());
    }
}
