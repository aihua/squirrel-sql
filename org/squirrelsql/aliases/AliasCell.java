package org.squirrelsql.aliases;

import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class AliasCell extends DndDragPositionMarkableTreeCell<AliasTreeNode>
{
   private Label _label = new Label();

   private Paint _stdTextFill;
   private AliasCutCopyState _aliasCutCopyState;
   private AliasTreeNodeChannel _aliasTreeNodeChannel;

   public AliasCell(AliasTreeNodeChannel aliasTreeNodeChannel, AliasCutCopyState aliasCutCopyState)
   {
      _aliasTreeNodeChannel = aliasTreeNodeChannel;
      _aliasCutCopyState = aliasCutCopyState;
      _aliasTreeNodeChannel.addListener(this::onTreeNodeChanged);
      _stdTextFill = _label.getTextFill();


      setOnDragDetected(this::onDragDetected);


      new DndDragPositionMarker<AliasTreeNode>(this);
   }


   private void onDragDetected(MouseEvent event)
   {
      if (isEmpty())
      {
         return;
      }
      Dragboard dragBoard = startDragAndDrop(TransferMode.MOVE);
      ClipboardContent content = new ClipboardContent();
      content.put(DataFormat.PLAIN_TEXT, "" + getItem());
      dragBoard.setContent(content);
      event.consume();
   }

   private void onTreeNodeChanged(TreeItem<AliasTreeNode> ti)
   {
      if(super.getTreeItem() == ti)
      {
         updateItem(ti.getValue(), false);
      }
   }


   @Override
   protected void updateItem(AliasTreeNode aliasTreeNode, boolean empty)
   {
      super.updateItem(aliasTreeNode, empty);

      if(empty)
      {
         setText(null);
         setGraphic(null);
         setTextFill(_stdTextFill);
         return;
      }

      setText(aliasTreeNode.getName());
      setTextFill(getTextFillColor(aliasTreeNode));
      setGraphic(getTreeItem().getGraphic());
   }




   private Paint getTextFillColor(AliasTreeNode aliasTreeNode)
   {
      if(null != _aliasCutCopyState.getTreeItemBeingCut() && aliasTreeNode == _aliasCutCopyState.getTreeItemBeingCut().getValue())
      {
         return Color.GRAY;
      }
      else
      {
         return _stdTextFill;
      }
   }
}
