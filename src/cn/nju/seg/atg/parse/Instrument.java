package cn.nju.seg.atg.parse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import cn.nju.seg.atg.visitor.CFGNode;

/**
 * 为目标程序生成插桩程序
 * @author zy
 *
 */
public class Instrument {

	/**
	 * 由函数转成的CFG树的开始节点
	 */
	public CFGNode cfgStartNode;
	
	/**
	 * 带参构造函数
	 * @param actionName
	 */
	public Instrument(CFGNode node)
	{
		this.cfgStartNode = node;
	}
	
	/**
	 * 插桩程序
	 * @param filePath
	 */
	public void instrumantation(String filePath)
	{
		Builder.nodePrinted = new ArrayList<Integer>();
		List<CFGNode> nodeList = new ArrayList<CFGNode>();
		this.getAllCFGNode(cfgStartNode, nodeList);
		this.sortCFGNodes(nodeList);
		int offsetIncreased = 0;
		int nodeListSize = nodeList.size();
		for (int i=0; i<nodeListSize; i++)
		{
			if (nodeList.get(i).getBinaryExpression() != null)
			{
				IASTBinaryExpression iabe = nodeList.get(i).getBinaryExpression();
				IASTNode[] iabeChildren = iabe.getChildren();
				if (iabeChildren.length == 2)
				{
					IASTExpression iaeLeft = (IASTExpression) iabeChildren[0];
					IASTExpression iaeRight = (IASTExpression) iabeChildren[1];
					String instrument = "printf(\"node%d %d %s if\\r\\n\",";
					if (nodeList.get(i).getSign() == 2)
					{
						instrument = "printf(\"node%d %d %s for\\r\\n\",";
					}					
					instrument += nodeList.get(i).getNodeNumber()
								  + ","
								  + "((" + iaeLeft.getRawSignature() + ")"
								  + "-"
								  + "(" + iaeRight.getRawSignature() + "))"
								  + ","
								  + "\"" + iabe.getRawSignature() + "\""
								  + "),";
					this.insertBeforeOffset(instrument, nodeList.get(i).getOffset()+offsetIncreased, filePath);
					offsetIncreased = offsetIncreased + instrument.length();
				}
			}
			else
			{
				String instrument = "printf(\"node%d\\r\\n\"," + nodeList.get(i).getNodeNumber() + ");\r\n\t";
				this.insertBeforeOffset(instrument, nodeList.get(i).getOffset()+offsetIncreased, filePath);
				offsetIncreased = offsetIncreased + instrument.length();
			}
		}
	}
	
	/**
	 * 获取所有的CFG节点（无序）
	 * 为程序插桩做准备
	 * @param node
	 * @param nodeList
	 */
	private void getAllCFGNode(CFGNode node, List<CFGNode> nodeList)
	{
		if (node != null)
		{
			if (node.getOffset() != -1 && !Builder.nodePrinted.contains(node.getOffset()))
			{
				CFGNode nodeTemp = new CFGNode();
				nodeTemp.setBinaryExpression(node.getBinaryExpression());
				nodeTemp.setNodeNumber(node.getNodeNumber());
				nodeTemp.setOffset(node.getOffset());
				nodeTemp.setSign(node.getSign());
				nodeList.add(nodeTemp);
			}
			Builder.nodePrinted.add(node.getOffset());
			
			//迭代
			List<CFGNode> children = node.getChildren();
			if (children != null)
			{
				for (int i=0; i<children.size(); i++)
				{
					if (!Builder.nodePrinted.contains(children.get(i).getOffset()))
					{
						this.getAllCFGNode(children.get(i), nodeList);
					}
				}
			}
			this.getAllCFGNode(node.getIfChild(), nodeList);
			this.getAllCFGNode(node.getElseChild(), nodeList);
		}
	}
	
	/**
	 * 对所有CFG节点按节点号从小到大排序
	 * @param nodeList
	 */
	private void sortCFGNodes(List<CFGNode> nodeList)
	{
		List<CFGNode> nodeListTemp = new ArrayList<CFGNode>();
		int nodeListSize = nodeList.size();
		for (int i=0; i<nodeListSize; i++)
		{
			nodeListTemp.add(null);
		}
		for (int i=0; i<nodeListSize; i++)
		{
			nodeListTemp.set(nodeList.get(i).getNodeNumber()-1, nodeList.get(i));
		}
		for (int i=0; i<nodeListSize; i++)
		{
			nodeList.set(i, nodeListTemp.get(i));
		}
	}
	
	/**
	 * 在CFG节点中插桩
	 * @param fragment
	 * @param offset
	 * @param filePath
	 */
	private void insertBeforeOffset(String fragment, int offset, String filePath)
	{
		try {
			RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
			int rafLength = (int)raf.length();
			byte[] bytes = new byte[rafLength-offset];
			raf.seek(offset);
			raf.read(bytes);

			//如果当前节点以{}包裹，则插入点调整到{后
			String str = new String(bytes);
			char[] charArray = str.toCharArray();
			int charTemp = -1;
			int numberOfBrace = 0;
			int i = 0;
			for (i=0; i<charArray.length; i++)
			{
				charTemp = (int)charArray[i];
				if (charTemp == 32 || charTemp == 10 || charTemp == 13 || charTemp == 9)
				{
					continue;
				}
				else if (charTemp == 123 && numberOfBrace <= 1)
				{
					numberOfBrace++;
					continue;
				}
				else
				{
					break;
				}
			}
			
			if (i > 0)
			{
				bytes = new byte[rafLength-offset-i];
				raf.seek(offset+i);
				raf.read(bytes);
				raf.seek(offset+i);
			}
			else
			{
				raf.seek(offset);
			}
			raf.write(fragment.getBytes());
			raf.write(bytes);
			raf.close();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
