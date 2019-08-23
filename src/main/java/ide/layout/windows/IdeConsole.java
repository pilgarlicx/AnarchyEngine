package ide.layout.windows;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.lwjgl.glfw.GLFW;

import engine.lua.LuaEngine;
import ide.layout.IdePane;
import ide.layout.windows.icons.Icons;
import lwjgui.LWJGUI;
import lwjgui.font.Font;
import lwjgui.geometry.Pos;
import lwjgui.scene.control.ContextMenu;
import lwjgui.scene.control.MenuItem;
import lwjgui.scene.control.SeparatorMenuItem;
import lwjgui.scene.control.TextArea;
import lwjgui.scene.control.TextField;
import lwjgui.scene.layout.BorderPane;

public class IdeConsole extends IdePane {

	public IdeConsole() {
		super("Console", true);
		
		BorderPane editBox = new BorderPane();
		editBox.setAlignment(Pos.TOP_LEFT);
		this.getChildren().add(editBox);

		TextArea console = new TextArea();
		console.setFont(Font.COURIER);
		console.setEditable(false);
		console.setFillToParentHeight(true);
		console.setFillToParentWidth(true);
		editBox.setCenter(console);
		
		TextField luaInput = new TextField();
		luaInput.setPrompt("Lua Command Line");
		luaInput.setFillToParentWidth(true);
		editBox.setBottom(luaInput);
		
		// Console context menu
		{
			ContextMenu menu = new ContextMenu();
			console.setContextMenu(menu);
			
			MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
			cut.setOnAction((event)->{
				if ( console.isEditing() )
					console.cut();
			});
			menu.getItems().add(cut);
			
			MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
			copy.setOnAction((event)->{
				console.copy();
			});
			menu.getItems().add(copy);

			MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
			paste.setOnAction((event)->{
				console.paste();
			});
			menu.getItems().add(paste);
			
			menu.getItems().add(new SeparatorMenuItem());
			
			MenuItem clear = new MenuItem("Clear Console");
			clear.setOnAction((event)->{
				console.clear();
			});
			menu.getItems().add(clear);
		}
		
		// Command line context menu
		{
			ContextMenu menu = new ContextMenu();
			luaInput.setContextMenu(menu);
			
			MenuItem cut = new MenuItem("Cut", Icons.icon_cut.getView());
			cut.setOnAction((event)->{
				if ( luaInput.isEditing() )
					luaInput.cut();
			});
			menu.getItems().add(cut);
			
			MenuItem copy = new MenuItem("Copy", Icons.icon_copy.getView());
			copy.setOnAction((event)->{
				luaInput.copy();
			});
			menu.getItems().add(copy);

			MenuItem paste = new MenuItem("Paste", Icons.icon_paste.getView());
			paste.setOnAction((event)->{
				luaInput.paste();
			});
			menu.getItems().add(paste);
		}

		this.setOnKeyPressed(event -> {
			if ( event.getKey() != GLFW.GLFW_KEY_ENTER )
				return;
			
			if ( cached_context.getSelected() == null )
				return;
			
			if (cached_context.getSelected().isDescendentOf(luaInput) ) {
				String lua = luaInput.getText();
				console.appendText("> " + lua+"\n");
				LuaEngine.runLua(lua);
				luaInput.setText("");
			}
		});
		
		// Print all text to console
		LuaEngine.globals.STDOUT = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				String letter = ""+(char)b;
				LWJGUI.runLater(() -> {
					console.appendText(letter);
				});
			}
		}, true);
		
		// Print all errors to console
		LuaEngine.globals.STDERR = new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				String letter = ""+(char)b;
				LWJGUI.runLater(() -> {
					console.appendText(letter);
				});
			}
		}, true);

		
	}

	@Override
	public void onOpen() {
		//
	}

	@Override
	public void onClose() {
		//
	}
}