package cn.nju.seg.atg.action;

import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import cn.nju.seg.atg.parse.AbstractAST;
import cn.nju.seg.atg.parse.Builder;

public class GenerateAction implements IObjectActionDelegate {

	private ISelection selection;
	
	/**
	 * Constructor for Action1.
	 */
	public GenerateAction() {
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			/*
			 * 如果当前Explorer中的“被选项”符合plugin.xml的<ObjectContribution>定义
			 * 即，在本插件中，只有“被选项”是函数(IFunctionDeclaration)是，才执行以下代码
			 */
			IFunctionDeclaration ifd = (IFunctionDeclaration) ((IStructuredSelection) selection).getFirstElement();
			createActionExuecutable(action.getId()).run(ifd);
		}
	}

	private AbstractAST createActionExuecutable(String id) {
		if ("cn.nju.seg.atg.cfgId".equals(id)) {
			return new Builder("cfg");
		} else if ("cn.nju.seg.atg.instrument".equals(id)) {
			return new Builder("instrument");
		} else {
			throw new IllegalArgumentException(id);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

	}

}
