package cn.nju.seg.atg.parse;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 * @author ChengXin
 * 
 */
public abstract class AbstractAST {
	
	public abstract void run(IFunctionDeclaration ifd);

	protected IASTTranslationUnit parse(ITranslationUnit lwUnit) {
		IIndex index = null;
		IASTTranslationUnit iatu = null;
		try {
			index = CCorePlugin.getIndexManager().getIndex(
				lwUnit.getCProject(), 
				IIndexManager.ADD_DEPENDENCIES|IIndexManager.ADD_DEPENDENT
				);
			index.acquireReadLock();
			iatu = lwUnit.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);
		} catch (CoreException ce) {
			ce.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}finally {
			index.releaseReadLock();
		}
		return iatu;
	}
	
	protected IASTTranslationUnit parse(IFunctionDeclaration ifd){
		try {
			Builder.funcName = ifd.getSignature();
		} catch (CModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ITranslationUnit itu = ifd.getTranslationUnit();
		IIndex index = null;
		IASTTranslationUnit iatu = null;		
		try {
			index = CCorePlugin.getIndexManager().getIndex(
				ifd.getCProject(), 
				IIndexManager.ADD_DEPENDENCIES|IIndexManager.ADD_DEPENDENT
				);
			index.acquireReadLock();
			iatu = itu.getAST(index, ITranslationUnit.AST_SKIP_ALL_HEADERS);			
		} catch (CoreException ce) {
			ce.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}finally {
			index.releaseReadLock();
		}
		return iatu;
	}
}