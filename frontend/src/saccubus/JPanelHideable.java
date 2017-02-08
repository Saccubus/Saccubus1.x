/**
 * added java source file
 */
package saccubus;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 * @author orz_e
 *
 */
public class JPanelHideable extends JPanel {
	private static ConcurrentHashMap<String, JPanelHideable> hideMap = new ConcurrentHashMap<>();
	// panel-name -> panel mapping, for status save
	private final JPanel contentPanel;
	private final JLabel hideLabel;
	private final JLabel showLabel;
	private boolean isContentVisible;
	private String panelID;
	/**
	 *
	 */
	private JPanelHideable() {
		super();
		contentPanel = new JPanel();
		contentPanel.setLayout(new GridBagLayout());
		hideLabel = new JLabel("��");
		hideLabel.setForeground(Color.red);
		showLabel = new JLabel("�\��");
		showLabel.setForeground(Color.blue);
		isContentVisible = true;
		FocusedHideSwitch adapter = new FocusedHideSwitch(this);
		hideLabel.addMouseListener(adapter);
		showLabel.addMouseListener(adapter);
	}
	private JPanelHideable(String title, Color col){
		this();
		super.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints(
			0, 0, 1, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH,
			new Insets(0, 0, 0, 0), 5, 5
		);
		super.add(contentPanel, c);
		showLabel.setVisible(false);
		super.add(showLabel, c);
		super.setBorder(BorderFactory.createTitledBorder(
				MainFrame.CREATE_ETCHED_BORDER,
				title, TitledBorder.LEADING, TitledBorder.TOP,
				getFont(), col));
		contentPanel.setLayout(new GridBagLayout());
		contentPanel.setVisible(true);
	}
	public JPanelHideable(String name, String title, Color col){
		this(title, col);
		panelID = name;
		hideMap.put(panelID, this);
	}

	public JLabel getHideLabel(){
		return hideLabel;
	}

	public Component add(Component comp){
		contentPanel.add(comp);
		return contentPanel;
	}

	public void add(Component comp, Object c){
		contentPanel.add(comp, c);
	}

	public void hidePanel(){
		isContentVisible = false;
		contentPanel.setVisible(false);
		showLabel.setVisible(true);
	}

	public void showPanel(){
		isContentVisible = true;
		showLabel.setVisible(false);
		contentPanel.setVisible(true);
	}

	public static String getHideMap(){
		StringBuffer sb = new StringBuffer();
		for(JPanelHideable p: hideMap.values()){
			if(!p.isContentVisible){
				sb.append(p.panelID);
				sb.append("\t");
			}
		}
		return sb.substring(0).trim();
	}

	public static void hidePanelAll() {
		for(JPanelHideable p: hideMap.values()){
			if(p!=null){
				p.hidePanel();
			}
		}
	}

	public static void showPanelAll() {
		for(JPanelHideable p: hideMap.values()){
			if(p!=null){
				p.showPanel();
			}
		}
	}

	public static void setHideMap(String hideMapString){
		if(hideMapString==null || hideMapString.isEmpty())
			return;
		String[] keys = hideMapString.split("\t");
		for(String key: keys){
			if(key.isEmpty())
				continue;
			JPanelHideable p = hideMap.get(key);
			if(p!=null)
				p.hidePanel();
		}
	}

	class FocusedHideSwitch extends MouseAdapter {
		private JPanelHideable target;

		FocusedHideSwitch(JPanelHideable comp){
			target = comp;
		}

		@Override
		public void  mouseClicked(MouseEvent e) {
			if(target.isContentVisible){
				target.hidePanel();
			} else {
				target.showPanel();
			}
		}
	}
}
