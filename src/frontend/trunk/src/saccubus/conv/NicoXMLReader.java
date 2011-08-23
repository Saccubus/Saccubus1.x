package saccubus.conv;

import java.util.regex.Pattern;

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
	private final Packet Packet;

	private Chat item;

	private boolean item_kicked;

	Object waitObject = new Object();

	private final Pattern NG_Word;

	private final Pattern NG_ID;

	public NicoXMLReader(Packet packet, String ng_id, String ng_word) {
		Packet = packet;
		NG_Word = makePattern(ng_word);
		NG_ID = makePattern(ng_id);
	}

	private static final Pattern makePattern(String word) {
		if (word == null || word.length() <= 0) {
			return null;
		}
		String tmp[] = word.split(" ");
		String tmp2[] = new String[tmp.length];
		int tmp_index = 0;
		int index;
		for (index = 0; index < tmp.length && tmp_index < tmp.length; index++) {
			if (tmp[tmp_index].startsWith("/")) {
				String str = tmp[tmp_index];
				for (tmp_index++; tmp_index < tmp.length; tmp_index++) {
					str += " " + tmp[tmp_index];
					if (tmp[tmp_index].endsWith("/")) {
						tmp_index++;
						break;
					}
				}
				tmp2[index] = str;
			} else if (tmp[tmp_index].startsWith("\"")) {
				String str = tmp[tmp_index];
				for (tmp_index++; tmp_index < tmp.length; tmp_index++) {
					str += " " + tmp[tmp_index];
					if (tmp[tmp_index].endsWith("\"")) {
						tmp_index++;
						break;
					}
				}
				tmp2[index] = str;
			} else {
				tmp2[index] = tmp[tmp_index];
				tmp_index++;
			}
		}
		String elt[] = new String[index];
		for (int i = 0; i < index; i++) {
			elt[i] = tmp2[i];
		}
		String reg = "";
		for (int i = 0; i < elt.length; i++) {
			String e = elt[i];
			System.out.println(e);
			if (i > 0) {
				reg += "|";
			}
			if (e.indexOf("/") == 0 && e.lastIndexOf("/") == e.length() - 1) {
				reg += "(" + e.substring(1, e.length() - 1) + ")";
			} else if (e.indexOf("\"") == 0
					&& e.lastIndexOf("\"") == e.length() - 1) {
				reg += "(" + Pattern.quote(e.substring(1, e.length() - 1))
						+ ")";
			} else {
				reg += "(.*(" + Pattern.quote(e) + ")+.*)";
			}
		}
		System.out.println("reg:" + reg);
		return Pattern.compile(reg);
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
		System.out.println("Start converting to interval file.");
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
			//マイメモリ削除対象
			String deleted = attributes.getValue("deleted");
			if(deleted != null && deleted.toLowerCase().equals("1")){
				item_kicked = true;
				return;
			}
			item.setDate(attributes.getValue("date"));
			String mail = attributes.getValue("mail");
			if (match(NG_Word, mail)) {
				item_kicked = true;
				return;
			}
			item.setMail(mail);
			item.setNo(attributes.getValue("no"));
			String user_id = attributes.getValue("user_id");
			if (match(NG_ID, user_id)) {
				item_kicked = true;
				return;
			}
			item.setUserID(user_id);
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
		char input[] = (new String(ch, offset, length)).toCharArray();
		for (int i = 0; i < input.length; i++) {
			if (!Character.isDefined(input[i])) {
				input[i] = '?';
			}
		}
		if (item != null) {
			String com = new String(input);
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
				Packet.addChat(item);
			}
			item = null;
		}
	}

	/**
	 * ドキュメント終了
	 */
	public void endDocument() {
		// System.out.println("----------");
		System.out.println("Converting finished.");
	}

}
