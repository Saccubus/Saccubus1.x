package saccubus.conv;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

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
public class NicoXMLReader extends DefaultHandler {
	private final Packet packet;

	private Chat item;

	private boolean item_kicked;

	//Object waitObject = new Object();

	private final Pattern NG_Word;

	private final Pattern NG_ID;

	private final CommandReplace NG_Cmd;

	private boolean item_fork;

	public NicoXMLReader(Packet packet, Pattern ngIdPat, Pattern ngWordPat, CommandReplace cmd){
		this.packet = packet;
		NG_Word = ngWordPat;
		NG_ID = ngIdPat;
		NG_Cmd = cmd;
	}

	public static final Pattern makePattern(String word) throws PatternSyntaxException{
		if (word == null || word.length() <= 0) {
			return null;
		}
		String tmp[] = word.split(" ");
		String tmp2[] = new String[tmp.length];
		int tmp_index = 0;
		int index;
		for (index = 0; index < tmp.length && tmp_index < tmp.length; index++) {
			if (tmp[tmp_index].startsWith("/")) {
				StringBuffer str = new StringBuffer(tmp[tmp_index]);
				for (tmp_index++; tmp_index < tmp.length; tmp_index++) {
					str.append(" " + tmp[tmp_index]);
					if (tmp[tmp_index].endsWith("/")) {
						tmp_index++;
						break;
					}
				}
				tmp2[index] = str.substring(0);
			} else if (tmp[tmp_index].startsWith("\"")) {
				StringBuffer str = new StringBuffer(tmp[tmp_index]);
				for (tmp_index++; tmp_index < tmp.length; tmp_index++) {
					str.append(" " + tmp[tmp_index]);
					if (tmp[tmp_index].endsWith("\"")) {
						tmp_index++;
						break;
					}
				}
				tmp2[index] = str.substring(0);
			} else {
				tmp2[index] = tmp[tmp_index];
				tmp_index++;
			}
		}
		String elt[] = new String[index];
		for (int i = 0; i < index; i++) {
			elt[i] = tmp2[i];
		}
		StringBuffer regb = new StringBuffer();
		for (int i = 0; i < elt.length; i++) {
			String e = elt[i];
			System.out.println(e);
			if (i > 0) {
				regb.append("|");
			}
			if (e.length() > 1 && e.indexOf("/") == 0 && e.lastIndexOf("/") == e.length() - 1) {
				regb.append("(" + e.substring(1, e.length() - 1) + ")");
			} else if (e.length() > 1 && e.indexOf("\"") == 0
					&& e.lastIndexOf("\"") == e.length() - 1) {
				regb.append("(" + Pattern.quote(e.substring(1, e.length() - 1))
						+ ")");
			} else {
				regb.append("(.*(" + Pattern.quote(e) + ")+.*)");
			}
		}
		String reg = regb.substring(0);
		System.out.println("reg:" + reg);
		Pattern pat;
		pat = Pattern.compile(reg);
		return pat;
	}

	private static final boolean match(Pattern pat, String word) {
		if (word == null || word.length() <= 0 || pat == null) {
			return false;
		}
		return pat.matcher(word).matches();
	}

	/**
	 *
	 */
	public void startDocument() {
		System.out.println("Start converting to intermediate file.");
	}

	/**
	 *
	 * @param uri
	 *            String
	 * @param localName
	 *            String
	 * @param qName
	 *            String
	 * @param attributes
	 *            Attributes
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) {
		if (qName.toLowerCase().equals("chat")) {
			// System.out.println("----------");
			item = new Chat();
			item_kicked = false;
			item_fork = false;
			//マイメモリ削除対象
			//運営削除対象
			String deleted = attributes.getValue("deleted");
			if("1".equals(deleted) || "2".equals(deleted)){
				item_kicked = true;
				return;
			}
			// item.setDate(attributes.getValue("date"));
			String mail = attributes.getValue("mail");
			//184（匿名）でない場合は、186（通知）を最後に付加
			if(mail==null){
				mail = "186";
			}else if(!mail.contains("184")){
				mail += " 186";
			}
			if (match(NG_Word, mail)) {
				item_kicked = true;
				return;
			}
			mail = NG_Cmd.replace(mail);
			item.setMail(mail);
			item.setNo(attributes.getValue("no"));
			String user_id = attributes.getValue("user_id");
			if (match(NG_ID, user_id)) {
				item_kicked = true;
				return;
			}
			String forkval = attributes.getValue("fork");
			if (forkval != null && forkval.equals("1")) {
				item_fork = true;
			}
			// item.setUserID(user_id);
			item.setVpos(attributes.getValue("vpos"));

		}
	}


	/**
	 *
	 * @param ch
	 *            char[]
	 * @param offset
	 *            int
	 * @param length
	 *            int
	 */
	public void characters(char[] ch, int offset, int length) {
		if (item != null) {
			if (item_fork && length > 0
					&& ch[offset] == '/'){	//ignore NIWANGO temporary
				item_kicked = true;
				return;
			}
			for (int i = offset; i < offset + length; i++) {
				if (!Character.isDefined(ch[i])) {
					ch[i] = '?';
				}
			}
			String com = new String(ch, offset, length);
			if (match(NG_Word, com)) {
				item_kicked = true;
				return;
			}
			item.setComment(com);
		}
	}

	/**
	 *
	 * @param uri
	 *            String
	 * @param localName
	 *            String
	 * @param qName
	 *            String
	 */
	public void endElement(String uri, String localName, String qName) {
		if (qName.toLowerCase().equals("chat")) {
			if (!item_kicked) {
				packet.addChat(item);
			}
			item = null;
		}
	}

	/**
	 * ドキュメント終了
	 */
	public void endDocument() {
		// System.out.println("----------");
		System.out.println("Converting finished. "
			+ packet.size() + " items.");
	}

}
