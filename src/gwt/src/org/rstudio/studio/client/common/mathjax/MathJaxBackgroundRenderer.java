/*
 * MathJaxBackgroundRenderer.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.mathjax;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.studio.client.common.mathjax.display.MathJaxPopupPanel;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorModeChangedEvent;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;

public class MathJaxBackgroundRenderer
{
   public MathJaxBackgroundRenderer(MathJax mathjax,
                                    MathJaxPopupPanel popup,
                                    DocDisplay docDisplay)
   {
      mathjax_ = mathjax;
      popup_ = popup;
      docDisplay_ = docDisplay;
      handlers_ = new ArrayList<HandlerRegistration>();
      
      renderTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            Range range = MathJaxUtil.getLatexRange(docDisplay_);
            if (range == null)
               return;
            
            mathjax_.renderLatex(range, true);
         }
      };
      
      handlers_.add(docDisplay_.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            beginMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addEditorModeChangedHandler(new EditorModeChangedEvent.Handler()
      {
         @Override
         public void onEditorModeChanged(EditorModeChangedEvent event)
         {
            beginMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addBlurHandler(new BlurHandler()
      {
         @Override
         public void onBlur(BlurEvent event)
         {
            endMonitoring();
         }
      }));
      
      handlers_.add(docDisplay_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (!event.isAttached())
               resetHandlers();
         }
      }));
   }
   
   private void beginMonitoring()
   {
      resetHandlers();
      String id = docDisplay_.getModeId();
      if (!id.equals("mode/rmarkdown"))
         return;
      
      cursorChangedHandler_ = docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
      {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            int delayMs = 700;
            if (MathJaxUtil.isSelectionWithinLatexChunk(docDisplay_))
               delayMs = 200;
            else if (popup_.isShowing())
               delayMs = 200;
            
            renderTimer_.schedule(delayMs);
         }
      });
   }
   
   private void endMonitoring()
   {
      resetHandlers();
   }
   
   private void resetHandlers()
   {
      for (HandlerRegistration handler : handlers_)
         handler.removeHandler();
      handlers_.clear();
      
      if (cursorChangedHandler_ != null)
      {
         cursorChangedHandler_.removeHandler();
         cursorChangedHandler_ = null;
      }
   }
   
   private final MathJax mathjax_;
   private final MathJaxPopupPanel popup_;
   private final DocDisplay docDisplay_;
   private final List<HandlerRegistration> handlers_;
   private final Timer renderTimer_;
   
   private HandlerRegistration cursorChangedHandler_;
}
