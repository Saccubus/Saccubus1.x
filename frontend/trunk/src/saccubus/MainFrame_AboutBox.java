package saccubus;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

//import psi.lib.swing.PopupRightClick;

/**
 * <p>
 * タイトル: さきゅばす
 * </p>
 *
 * <p>
 * 説明: ニコニコ動画の動画をコメントつきで保存
 * </p>
 *
 * <p>
 * 著作権: Copyright (c) 2007 PSI
 * </p>
 *
 * <p>
 * 会社名:
 * </p>
 *
 * @author 未入力
 * @version 1.0
 */
public class MainFrame_AboutBox extends JDialog implements ActionListener {
	/**
	 *
	 */
	private static final long serialVersionUID = -4256413309312729840L;

	String version = "ver1.22r(2008/04/27)";

	public static final String rev = "1.22r3e8";
	private static final String modefied = "(2011/08/18)";

	String productHTML =
		"<html><p>本プログラムは<br/>" +
		"  <a href=\'http://www.ne.jp/asahi/mochiyama/my/file/Saccubus-1.22r3.zip\'>" +
		"「さきゅばす 1.22r3」</a><br/>" +
		"  <a href=\'http://sourceforge.jp/projects/coroid/downloads/48371/saccubus_nibr1.4.4.zip/\'>" +
		"「さきゅばすNicoBrowser拡張1.4.4」</a><br/>" +
		"を改造したものです。<br/>" +
		" <a href=\'http://sourceforge.jp/projects/coroid/downloads/52940/inqubus1.7.0.zip/\'>"	+
		"「いんきゅばす1.7.0」</a>　も参考にしています。<br/>" +
		" orz " + rev + " " + modefied + "<br/>"+
		"上記および本プログラムのオリジナルは以下の通りです。<br/><br/>"+
		"<a href=\'http://sourceforge.jp/projects/saccubus/downloads/30757/Saccubus-1.22r.zip/\'>" +
		"さきゅばす<br/>"+
		version + "</a><br/><br/>"+
		"<table border=0>" +
		"<tr><td>Copyright (C) <td>2008 Saccubus Developers Team"+
		"<tr><td><td>2007-2008 PSI"+
		"</table><br/>" +
		"ニコニコ動画の動画をコメントつきで保存</p>" +
		"<p>関連リンク<br/>" +
		"<a href=\'http://sourceforge.jp/projects/saccubus/\'>さきゅばす project</a><br/>" +
		"<a href=\'http://sourceforge.jp/projects/coroid/\'>coroid project（いんきゅばすを含む）</a>" +
		"</p></html>";

	JPanel panel1 = new JPanel();

	JPanel panel2 = new JPanel();

	JPanel insetsPanel1 = new JPanel();

	JPanel insetsPanel2 = new JPanel();

	JPanel insetsPanel3 = new JPanel();

	JEditorPane editorPane;

	JButton button1 = new JButton();

	JLabel imageLabel = new JLabel();
/*
	JTextArea product_field = new JTextArea(product);
*/
	ImageIcon image1 = new ImageIcon();

	BorderLayout borderLayout1 = new BorderLayout();

	BorderLayout borderLayout2 = new BorderLayout();

	FlowLayout flowLayout1 = new FlowLayout();

	GridLayout gridLayout1 = new GridLayout();

	public MainFrame_AboutBox(Frame parent) {
		super(parent);
		try {
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public MainFrame_AboutBox() {
		this(null);
	}

	/**
	 * コンポーネントの初期化。
	 *
	 * @throws java.lang.Exception
	 */
	private void jbInit() throws Exception {
		image1 = new ImageIcon(saccubus.MainFrame.class.getResource("icon.png"));
		imageLabel.setIcon(image1);
		setTitle("バージョン情報");
		panel1.setLayout(borderLayout1);
		panel2.setLayout(borderLayout2);
		insetsPanel1.setLayout(flowLayout1);
		insetsPanel2.setLayout(flowLayout1);
		insetsPanel2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		gridLayout1.setRows(1);
		gridLayout1.setColumns(1);
		insetsPanel3.setLayout(gridLayout1);
		insetsPanel3.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		button1.setText("OK");
		button1.addActionListener(this);
		insetsPanel2.add(imageLabel, null);
		panel2.add(insetsPanel2, BorderLayout.WEST);
		getContentPane().add(panel1, null);
/*
		product_field.setForeground(insetsPanel3.getForeground());
		product_field.setBackground(insetsPanel3.getBackground());
		product_field.addMouseListener(new PopupRightClick(product_field));
		product_field.setEditable(false);
*/
		editorPane = new JEditorPane("text/html", productHTML);
		editorPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		editorPane.setEditable(false);
		editorPane.setOpaque(false);
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == EventType.ACTIVATED){
					URL url = e.getURL();
					Desktop desktop = Desktop.getDesktop();
					try {
						desktop.browse(url.toURI());
					} catch (IOException ex) {
						ex.printStackTrace();
					} catch (URISyntaxException ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		insetsPanel3.add(editorPane, null);
		panel2.add(insetsPanel3, BorderLayout.CENTER);
		insetsPanel1.add(button1, null);
		panel1.add(insetsPanel1, BorderLayout.SOUTH);
		panel1.add(panel2, BorderLayout.NORTH);
		setResizable(true);
	}

	/**
	 * ボタンイベントでダイアログを閉じる
	 *
	 * @param actionEvent
	 *            ActionEvent
	 */
	@Override
	public void actionPerformed(ActionEvent actionEvent) {
		if (actionEvent.getSource() == button1) {
			dispose();
		}
	}
}
