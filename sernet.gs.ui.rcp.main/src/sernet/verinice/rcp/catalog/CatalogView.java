/*******************************************************************************
 * Copyright (c) 2017 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Sebastian Hagedorn sh[at]sernet.de - initial API and implementation
 *     Urs Zeidler uz[at]sernet.de - initial API and implementation
 ******************************************************************************/
package sernet.verinice.rcp.catalog;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.DrillDownAdapter;

import sernet.gs.service.NumericStringComparator;
import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.ImageCache;
import sernet.gs.ui.rcp.main.bsi.dnd.transfer.BaseProtectionModelingTransfer;
import sernet.gs.ui.rcp.main.bsi.editors.BSIElementEditor;
import sernet.gs.ui.rcp.main.bsi.editors.BSIElementEditorInput;
import sernet.gs.ui.rcp.main.bsi.editors.EditorFactory;
import sernet.gs.ui.rcp.main.bsi.editors.EditorRegistry;
import sernet.gs.ui.rcp.main.common.model.CnAElementFactory;
import sernet.gs.ui.rcp.main.common.model.DefaultModelLoadListener;
import sernet.gs.ui.rcp.main.common.model.IModelLoadListener;
import sernet.gs.ui.rcp.main.preferences.PreferenceConstants;
import sernet.hui.common.connect.HUITypeFactory;
import sernet.verinice.interfaces.ActionRightIDs;
import sernet.verinice.iso27k.rcp.ILinkedWithEditorView;
import sernet.verinice.iso27k.rcp.JobScheduler;
import sernet.verinice.iso27k.rcp.LinkWithEditorPartListener;
import sernet.verinice.iso27k.rcp.Messages;
import sernet.verinice.iso27k.rcp.action.CollapseAction;
import sernet.verinice.iso27k.rcp.action.ExpandAction;
import sernet.verinice.iso27k.rcp.action.HideEmptyFilter;
import sernet.verinice.iso27k.rcp.action.ISMViewFilter;
import sernet.verinice.model.bp.elements.BpRequirement;
import sernet.verinice.model.bp.elements.Safeguard;
import sernet.verinice.model.catalog.CatalogModel;
import sernet.verinice.model.catalog.ICatalogModelListener;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.common.TagParameter;
import sernet.verinice.model.common.TypeParameter;
import sernet.verinice.rcp.IAttachedToPerspective;
import sernet.verinice.rcp.RightsEnabledView;
import sernet.verinice.rcp.bp.BaseProtectionPerspective;
import sernet.verinice.rcp.tree.TreeContentProvider;
import sernet.verinice.rcp.tree.TreeLabelProvider;
import sernet.verinice.rcp.tree.TreeUpdateListener;
import sernet.verinice.service.tree.ElementManager;

/**
 * @author Urs Zeidler uz[at]sernet.de
 *
 */
public class CatalogView extends RightsEnabledView implements IAttachedToPerspective, ILinkedWithEditorView {

    private static final Logger LOG = Logger.getLogger(CatalogView.class);

    protected TreeViewer viewer;
    private TreeContentProvider contentProvider;
    private ElementManager elementManager;
    private Lock lock = new ReentrantLock();
    private IModelLoadListener modelLoadListener;
    private ICatalogModelListener modelUpdateListener;
    private DrillDownAdapter drillDownAdapter;
    private Action doubleClickAction;
    private ExpandAction expandAction;
    private CollapseAction collapseAction;
    private ISMViewFilter filterAction;
    private Action linkWithEditorAction;
    private IPartListener2 linkWithEditorPartListener  = new LinkWithEditorPartListener(this);

    private boolean linkingActive;

    public static final String ID = "sernet.verinice.rcp.catalog.CatalogView"; //$NON-NLS-1$

    public CatalogView() {
        super();
        elementManager = new ElementManager();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.
     * widgets.Composite)
     */
    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);
        try {
            initView(parent);
            startInitDataJob();
        } catch (Exception e) {
            LOG.error("Error while creating organization view", e); //$NON-NLS-1$
            ExceptionUtil.log(e, Messages.ISMView_2);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see sernet.verinice.rcp.RightsEnabledView#getRightID()
     */
    @Override
    public String getRightID() {
        return ActionRightIDs.CATALOGVIEW;
    }

    /*
     * (non-Javadoc)
     * 
     * @see sernet.verinice.rcp.RightsEnabledView#getViewId()
     */
    @Override
    public String getViewId() {
        return ID;
    }

    /**
     * @param parent
     */
    protected void initView(Composite parent) {
        if (CnAElementFactory.isModernizedBpCatalogLoaded()) {
            CnAElementFactory.getInstance().reloadModelFromDatabase();
        }

        contentProvider = new TreeContentProvider(elementManager);
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        viewer.setSorter(new ModITBCatalogViewerSorter());
        drillDownAdapter = new DrillDownAdapter(viewer);
        viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(new TreeLabelProvider());

        toggleLinking(Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.LINK_TO_EDITOR));

        getSite().setSelectionProvider(viewer);
        hookContextMenu();
        makeActions();
        addActions();
        fillToolBar();
        addDndListeners();

        getSite().getPage().addPartListener(linkWithEditorPartListener);
        viewer.refresh(true);
    }

    private void addDndListeners() {
        int ops = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transfers = new Transfer[] { BaseProtectionModelingTransfer.getInstance() };
        viewer.addDragSupport(ops, transfers, new CatalogDragListener(viewer));
    }

    protected void startInitDataJob() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Catalogview: startInitDataJob"); //$NON-NLS-1$
        }
        WorkspaceJob initDataJob = new WorkspaceJob(Messages.ISMView_InitData) {
            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) {
                IStatus status = Status.OK_STATUS;
                try {
                    monitor.beginTask(Messages.ISMView_InitData, IProgressMonitor.UNKNOWN);
                    initData();
                } catch (Exception e) {
                    LOG.error("Error while loading data for catalog view.", e); //$NON-NLS-1$
                    status = new Status(Status.ERROR, "sernet.gs.ui.rcp.main", Messages.ISMView_4, e); //$NON-NLS-1$
                } finally {
                    monitor.done();
                }
                return status;
            }
        };
        JobScheduler.scheduleInitJob(initDataJob);
    }

    protected void initData() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("MotITBPCatalogVIEW: initData"); //$NON-NLS-1$
        }
        lock.lock();
        try {
            if (CnAElementFactory.isModernizedBpCatalogLoaded()) {
                if (modelUpdateListener == null) {
                    if (LOG.isDebugEnabled()) {
                        Logger.getLogger(this.getClass()).debug("Creating modelUpdateListener for MotITBPView."); //$NON-NLS-1$
                    }
                    modelUpdateListener = new TreeUpdateListener(viewer, elementManager);
                    CnAElementFactory.getInstance().getCatalogModel().addCatalogModelListener(modelUpdateListener);//
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            setInput(CnAElementFactory.getInstance().getCatalogModel());
                            viewer.refresh();
                        }
                    });
                }
            } else if (modelLoadListener == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("CatalogModel No model loaded, adding model load listener."); //$NON-NLS-1$
                }
                // model is not loaded yet: add a listener to load data when
                // it's loaded
                modelLoadListener = new DefaultModelLoadListener(){
                    @Override
                    public void loaded(CatalogModel model) {
                        startInitDataJob();
                    }  
                };
                CnAElementFactory.getInstance().addLoadListener(modelLoadListener);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set the input to the viewer.
     * 
     * @param catalogModel
     */
    public void setInput(CatalogModel catalogModel) {
        viewer.setInput(catalogModel);
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        
        viewer.getControl().setMenu(menu);
    }

    protected void fillContextMenu(IMenuManager manager) {
        manager.add(new GroupMarker("content")); //$NON-NLS-1$
        manager.add(new Separator());
        manager.add(doubleClickAction);
        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    private void makeActions() {
        doubleClickAction = new Action() {
            @Override
            public void run() {
                if (viewer.getSelection() instanceof IStructuredSelection) {
                    Object sel = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                    try {
                        openEditorReadOnly(sel);
                    } catch (PartInitException e) {
                        LOG.error("Error opening the BSIElement editor for: " + sel, e); //$NON-NLS-1$
                    }
                }
            }
        };
        doubleClickAction.setText(Messages.CatalogView_open_in_editor);
        doubleClickAction.setImageDescriptor(ImageCache.getInstance().getImageDescriptor(ImageCache.OPEN_EDIT));

        expandAction = new ExpandAction(viewer, contentProvider);
        expandAction.setText(Messages.ISMView_7);
        expandAction.setImageDescriptor(ImageCache.getInstance().getImageDescriptor(ImageCache.EXPANDALL));

        collapseAction = new CollapseAction(viewer);
        collapseAction.setText(Messages.ISMView_8);
        collapseAction.setImageDescriptor(ImageCache.getInstance().getImageDescriptor(ImageCache.COLLAPSEALL));

        HideEmptyFilter hideEmptyFilter = new HideEmptyFilter(viewer);
        hideEmptyFilter.setHideEmpty(true);
        TagParameter tagParameter = new TagParameter();
        TypeParameter typeParameter = new TypeParameter();
        filterAction = new ISMViewFilter(viewer, Messages.ISMView_12, tagParameter, hideEmptyFilter, typeParameter);

        linkWithEditorAction = new Action(Messages.ISMView_5, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                toggleLinking(isChecked());
            }
        };
        linkWithEditorAction.setChecked(isLinkingActive());
        linkWithEditorAction.setImageDescriptor(ImageCache.getInstance().getImageDescriptor(ImageCache.LINKED));
    }

    protected void openEditorReadOnly(Object sel) throws PartInitException {
        CnATreeElement element = (CnATreeElement) sel;
        IEditorPart editor = EditorRegistry.getInstance().getOpenEditor(element.getId());
        if (editor == null) {
            EditorFactory.getInstance().updateAndOpenObject(element, true);
        } else {
            getSite().getPage().openEditor(editor.getEditorInput(), BSIElementEditor.EDITOR_ID);
        }
    }


    private void addActions() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });

        viewer.addSelectionChangedListener(expandAction);
        viewer.addSelectionChangedListener(collapseAction);
    }

    protected void fillToolBar() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager manager = bars.getToolBarManager();
        manager.add(expandAction);
        manager.add(collapseAction);
        drillDownAdapter.addNavigationActions(manager);
        manager.add(filterAction);
        manager.add(linkWithEditorAction);
    }

    protected void toggleLinking(boolean checked) {
        this.linkingActive = checked;
        if (checked) {
            editorActivated(getSite().getPage().getActiveEditor());
        }
    }

    protected boolean isLinkingActive() {
        return linkingActive;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * sernet.verinice.iso27k.rcp.ILinkedWithEditorView#editorActivated(org.
     * eclipse.ui.IEditorPart)
     */
    @Override
    public void editorActivated(IEditorPart activeEditor) {
        if (!isLinkingActive() || !getViewSite().getPage().isPartVisible(this)) {
            return;
        }
        CnATreeElement element = BSIElementEditorInput.extractElement(activeEditor);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Element in editor: " + element.getUuid()); //$NON-NLS-1$
        }
        if(element != null){
            viewer.setSelection(new StructuredSelection(element),true);
        } else {
            return;
        }
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Tree is expanded."); //$NON-NLS-1$
        } 
    }

    @Override
    public String getPerspectiveId() {
        return BaseProtectionPerspective.ID;
    }

    @Override
    public void dispose() {
        elementManager.clearCache();
        if (CnAElementFactory.isModernizedBpCatalogLoaded()) {
            CnAElementFactory.getInstance().getCatalogModel().removeCatalogModelListener(modelUpdateListener);
        }
        CnAElementFactory.getInstance().removeLoadListener(modelLoadListener);
        getSite().getPage().removePartListener(linkWithEditorPartListener);
        super.dispose();
    }

    class ModITBCatalogViewerSorter extends ViewerSorter {
        private static final String HIGH = "HIGH";
        private static final String STANDARD = "STANDARD";
        private static final String BASIC = "BASIC";
        
        private NumericStringComparator comp = new NumericStringComparator();

        @Override
        public int compare(Viewer viewer, Object o1, Object o2) {
            int result = 0;
            if (o1 instanceof Safeguard  && o2 instanceof Safeguard) {
                Safeguard sg1 = (Safeguard) o1;
                Safeguard sg2 = (Safeguard) o2;
                result = quallifierToValue(sg1.getQualifier())-quallifierToValue(sg2.getQualifier());
            }
            if (o1 instanceof BpRequirement && o2 instanceof BpRequirement && result == 0) {
                BpRequirement br1 = (BpRequirement) o1;
                BpRequirement br2 = (BpRequirement) o2;
                result = quallifierToValue(br1.getQualifier())-quallifierToValue(br2.getQualifier());
            }
            if (o1 instanceof CnATreeElement && o2 instanceof CnATreeElement && result == 0) {
                CnATreeElement element1 = (CnATreeElement) o1;
                CnATreeElement element2 = (CnATreeElement) o2;
                String message1 = HUITypeFactory.getInstance().getMessage(element1.getTypeId());
                String message2 = HUITypeFactory.getInstance().getMessage(element2.getTypeId());
                result = comp.compare(message1, message2);

                if (result == 0) {
                    result = comp.compare(element1.getTitle(), element2.getTitle());
                }
            }
            return result;
        }
        
        private int quallifierToValue(String qualifier) {
            if(BASIC.equals(qualifier)){
                return 1;
            }else if(STANDARD.equals(qualifier)){
                return 2;
            }else if(HIGH.equals(qualifier)){
                return 3;
            }
            return 0;
        }

    }
}
