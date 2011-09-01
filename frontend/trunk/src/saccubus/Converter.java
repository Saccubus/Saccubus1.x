package saccubus;

import javax.swing.JLabel;

import saccubus.net.BrowserInfo;
import saccubus.net.BrowserInfo.BrowserCookieKind;
import saccubus.net.NicoClient;
import saccubus.net.Path;

import java.io.*;

import saccubus.conv.CombineXML;
import saccubus.conv.ConvertToVideoHook;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import saccubus.util.Cws2Fws;
import saccubus.util.Stopwatch;
import saccubus.ConvertingSetting;
import saccubus.FFmpeg.Aspect;

/**
 * <p>�^�C�g��: ������΂�</p>
 *
 * <p>����: �j�R�j�R����̓�����R�����g���ŕۑ�</p>
 *
 * <p>���쌠: Copyright (c) 2007 PSI</p>
 *
 * <p>��Ж�: </p>
 *
 * @author ������
 * @version 1.0
 */
public class Converter extends Thread {
	private ConvertingSetting Setting;
	private String Tag;
	private String VideoID;
	private String VideoTitle;
	private String Time;
	private JLabel Status;
	private final ConvertStopFlag StopFlag;
	private static final String TMP_COMMENT = "_vhook.tmp";
	private static final String TMP_OWNERCOMMENT = "_vhookowner.tmp";
	private static final String VIDEO_URL_PARSER = "http://www.nicovideo.jp/watch/";
	public static final String OWNER_EXT = "[Owner].xml";	// ���e�҃R�����g�T�t�B�b�N�X
	private static final String TMP_COMBINED_XML = "_tmp_comment.xml";
	private String OtherVideo;
	private final String WatchInfo;
	private final JLabel MovieInfo;
	private BrowserCookieKind BrowserKind = BrowserCookieKind.NONE;
	private final BrowserInfo BrowserInfo = new BrowserInfo();
	private String UserSession = "";	//�u���E�U����擾�������[�U�[�Z�b�V����
	private final Stopwatch Stopwatch;
	private File selectedVhook;
	private Aspect videoAspect;
	private File fwsFile = null;

	public Converter(String url, String time, ConvertingSetting setting,
			JLabel status, ConvertStopFlag flag, JLabel movieInfo, JLabel watch) {
		url = url.trim();
		if(url.startsWith(VIDEO_URL_PARSER)){
			url = url.substring(VIDEO_URL_PARSER.length());
		}
		int index = url.indexOf('?');
		if(index >= 0){
			Tag = url.substring(0,index);
			WatchInfo = url.substring(index);
		}else{
			Tag = url;
			WatchInfo = "";
		}
		VideoID = "[" + Tag + "]";
		if (time.equals("000000") || time.equals("0")){		// for auto.bat
			Time = "";
		} else {
			Time = time;
		}
		Setting = setting;
		Status = status;
		StopFlag = flag;
		MovieInfo = movieInfo;
		MovieInfo.setText(" ");
		Stopwatch = new Stopwatch(watch);
	}

	private File VideoFile = null;
	private File CommentFile = null;
	private File OwnerCommentFile = null;
	private File ConvertedVideoFile = null;
	private File CommentMiddleFile = null;
	private File OwnerMiddleFile = null;
	private FFmpeg ffmpeg = null;
	private File VhookNormal = null;
	private File VhookWide = null;
	private int wayOfVhook = 0;
	private ArrayList<File> listOfCommentFile = null;

	public File getVideoFile() {
		return VideoFile;
	}
	public ConvertingSetting getSetting(){
		return Setting;
	}

	private void sendtext(String text){
		Status.setText(text);
	}

	private boolean isSaveConverted(){
		return Setting.isSaveConverted();
	}
	private boolean isSaveVideo(){
		return Setting.isSaveVideo();
	}
	private boolean isSaveComment(){
		return Setting.isSaveComment();
	}
	private boolean isSaveOwnerComment(){
		return Setting.isSaveOwnerComment();
	}
	private boolean isConvertWithComment(){
		return Setting.isConvertWithComment();
	}
	private boolean isConvertWithOwnerComment(){
		return Setting.isConvertWithOwnerComment();
	}
	private boolean isVideoFixFileName(){
		return Setting.isVideoFixFileName();
	}
	private boolean isCommentFixFileName(){
		return Setting.isCommentFixFileName();
	}
	private String getProxy(){
		return Setting.getProxy();
	}
	private int getProxyPort(){
		return Setting.getProxyPort();
	}
	private String getMailAddress(){
		return Setting.getMailAddress();
	}
	private String getPassword(){
		return Setting.getPassword();
	}
	private boolean isDeleteVideoAfterConverting(){
		return Setting.isDeleteVideoAfterConverting();
	}
	private boolean isDeleteCommentAfterConverting(){
		return Setting.isDeleteCommentAfterConverting();
	}
	private boolean useProxy(){
		return Setting.useProxy();
	}

	private boolean checkOK() {
		sendtext("�`�F�b�N���Ă��܂�");
		if (!isSaveConverted() && !isSaveVideo()
			&& !isSaveComment() && !isSaveOwnerComment()){
			sendtext("�������邱�Ƃ�����܂���");
			return false;
		}
		if (isSaveConverted()) {
			File a = new File(Setting.getFFmpegPath());
			if (!a.canRead()) {
				sendtext("FFmpeg��������܂���B");
				return false;
			}
			this.ffmpeg = new FFmpeg(Setting.getFFmpegPath());
			if (Setting.isUseVhookNormal()){
				if(Setting.getVhookPath().indexOf(' ') >= 0) {
					sendtext("�����܂���B����vhook���C�u�����ɂ͔��p�󔒂͎g���܂���B");
					return false;
				}
				VhookNormal = new File(Setting.getVhookPath());
				if (!VhookNormal.canRead()) {
					sendtext("Vhook���C�u������������܂���B");
					return false;
				}
				wayOfVhook++;
			}
			if (Setting.isUseVhookWide()){
				if(Setting.getVhookWidePath().isEmpty()){
					VhookWide = VhookNormal;
				}
				else {
					if(Setting.getVhookWidePath().indexOf(' ') >= 0) {
						sendtext("�����܂���B����vhook�t�@�C�����ɂ͔��p�󔒂͎g���܂���B");
						return false;
					}
					VhookWide = new File(Setting.getVhookWidePath());
				}
				if (!VhookWide.canRead()) {
					sendtext("Vhook���C�u�����i���C�h�j��������܂���B");
					return false;
				}
				wayOfVhook++;
			}
			if (wayOfVhook == 0){
				sendtext("�g�p�ł���Vhook���C�u����������܂���B");
				return false;
			}
			a = new File(Setting.getFontPath());
			if (!a.canRead()) {
				sendtext("�t�H���g��������܂���B");
				return false;
			}
//			if (!detectOption()) {
//				sendtext("�ϊ��I�v�V�����t�@�C���̓ǂݍ��݂Ɏ��s���܂����B");
//				return false;
//			}
		} else {
			if (isDeleteVideoAfterConverting()) {
				sendtext("�ϊ����Ȃ��̂ɁA����폜��������ėǂ���ł����H");
				return false;
			}
			if (isDeleteCommentAfterConverting()) {
				sendtext("�ϊ����Ȃ��̂ɁA�R�����g�폜��������ėǂ���ł����H");
				return false;
			}
		}
		if (isSaveVideo() ||
				isSaveComment() || isSaveOwnerComment()) {
			// �u���E�U�Z�b�V�������L�̏ꍇ�͂����ŃZ�b�V������ǂݍ���
			if (Setting.isBrowserIE()){
				BrowserKind = BrowserCookieKind.MSIE;
				UserSession = BrowserInfo.getUserSession(BrowserKind);
			}
			if (UserSession.isEmpty() && Setting.isBrowserFF()){
				BrowserKind = BrowserCookieKind.Firefox;
				UserSession = BrowserInfo.getUserSession(BrowserKind);
			}
			if (UserSession.isEmpty() && Setting.isBrowserChrome()){
				BrowserKind = BrowserCookieKind.Chrome;
				UserSession = BrowserInfo.getUserSession(BrowserKind);
			}
			if (UserSession.isEmpty() && Setting.isBrowserChromium()){
				BrowserKind = BrowserCookieKind.Chromium;
				UserSession = BrowserInfo.getUserSession(BrowserKind);
			}
			if (UserSession.isEmpty() && Setting.isBrowserOpera()){
				BrowserKind = BrowserCookieKind.Opera;
				UserSession = BrowserInfo.getUserSession(BrowserKind);
			}
			if (UserSession.isEmpty() && Setting.isBrowserOther()){
				BrowserKind = BrowserCookieKind.Other;
				UserSession = BrowserInfo.getUserSessionOther(Setting.getBrowserCookiePath());
			}
			if (BrowserKind != BrowserCookieKind.NONE && UserSession.isEmpty()){
				sendtext("�u���E�U�@" + BrowserKind.toString()
						+ "�@�̃Z�b�V�����擾�Ɏ��s");
				return false;
			}
			if (BrowserKind == BrowserCookieKind.NONE
				&& (getMailAddress() == null
					|| getMailAddress().isEmpty()
					|| getPassword() == null
					|| getPassword().isEmpty())) {
				sendtext("���[���A�h���X���p�X���[�h���󔒂ł��B");
				return false;
			}
			if (useProxy()
				&& (   getProxy() == null || getProxy().isEmpty()
					|| getProxyPort() < 0 || getProxyPort() > 65535   )){
				sendtext("�v���L�V�̐ݒ肪�s���ł��B");
				return false;
			}
		}
		sendtext("�`�F�b�N�I��");
		return true;
	}

	private NicoClient getNicoClient() {
		if (isSaveVideo() || isSaveComment() || isSaveOwnerComment()) {
			String proxy = null;
			int proxy_port = -1;
			if (useProxy()) {
				proxy = getProxy();
				proxy_port = getProxyPort();
			}
			sendtext("���O�C����");
			NicoClient client = null;
			if (BrowserKind != BrowserCookieKind.NONE){
				// �Z�b�V�������L�A���O�C���ς݂�NicoClient��client�ɕԂ�
				client = new NicoClient(BrowserKind, UserSession, proxy, proxy_port, Stopwatch);
			} else {
				client = new NicoClient(getMailAddress(), getPassword(), proxy, proxy_port, Stopwatch);
			}
			if (!client.isLoggedIn()) {
				sendtext("���O�C�����s " + BrowserInfo.getBrowserName() + " " + client.getExtraError());
			} else {
				sendtext("���O�C������ " + BrowserInfo.getBrowserName());
			}
			return client;
		} else {
			return null;
		}
	}

	private boolean saveVideo(NicoClient client) {
		File folder = Setting.getVideoFixFileNameFolder();
		sendtext("����̕ۑ�");
		/*����̕ۑ�*/
		if (isSaveVideo()) {
			if (isVideoFixFileName()) {
				if (folder.mkdir()) {
					System.out.println("Folder created: " + folder.getPath());
				}
				if (!folder.isDirectory()) {
					sendtext("����̕ۑ���t�H���_���쐬�ł��܂���B");
					return false;
				}
				VideoFile = new File(folder,
						VideoID + VideoTitle + ".flv");
			} else {
				VideoFile = Setting.getVideoFile();
			}
			sendtext("����̃_�E�����[�h�J�n��");
			if (client == null){
				sendtext("���O�C�����ĂȂ��̂ɓ���̕ۑ��ɂȂ�܂���");
				return false;
			}
			VideoFile = client.getVideo(VideoFile, Status, StopFlag);
			if (stopFlagReturn()) {
				return false;
			}
			if (VideoFile == null) {
				sendtext("����̃_�E�����[�h�Ɏ��s");
				return false;
			}
		} else {
			if (isSaveConverted()) {
				if (isVideoFixFileName()) {
					String videoFilename;
					if((videoFilename = detectTitleFromVideo(folder)) == null){
						if (OtherVideo == null){
							sendtext("����t�@�C�����t�H���_�ɑ��݂��܂���B");
						} else {
							sendtext("����t�@�C����.flv�ł���܂���F" + OtherVideo);
						}
						return false;
					}
					VideoFile = new File(folder, videoFilename);
					if (!VideoFile.canRead()) {
						sendtext("����t�@�C�����ǂݍ��߂܂���B");
						return false;
					}
				} else {
					VideoFile = Setting.getVideoFile();
					if (!VideoFile.exists()) {
						sendtext("����t�@�C�������݂��܂���B");
						return false;
					}
				}
			}
		}
		sendtext("����̕ۑ����I��");
		return true;
	}

	private boolean saveComment(NicoClient client) {
		sendtext("�R�����g�̕ۑ�");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isSaveComment()) {
			if (isCommentFixFileName()) {
				if (folder.mkdir()) {
					System.out.println("Folder created: " + folder.getPath());
				}
				if (!folder.isDirectory()) {
					sendtext("�R�����g�̕ۑ���t�H���_���쐬�ł��܂���B");
					return false;
				}
				String ext = ".xml";
				if (Setting.isAddTimeStamp()) {
					if(Time == null || Time.isEmpty()
						|| Time.equals("Owner")){
						ext = "[" + WayBackDate.formatNow() + "]" + ext;
					} else {
						WayBackDate wbDate = new WayBackDate(Time);
						if (wbDate.isValid()){
							ext = "[" + wbDate.format() + "]" + ext;
						} else {
							ext = "[" + Time + "]" + ext;
						}
					}
				}
				CommentFile = new File(folder, VideoID + VideoTitle + ext);
			} else {
				CommentFile = Setting.getCommentFile();
			}
			if (client == null){
				sendtext("���O�C�����ĂȂ��̂ɃR�����g�̕ۑ��ɂȂ�܂���");
				return false;
			}
			String back_comment = Setting.getBackComment();
			if (Setting.isFixCommentNum()) {
				back_comment = client
						.getBackCommentFromLength(back_comment);
			}
			sendtext("�R�����g�̃_�E�����[�h�J�n��");
			CommentFile = client.getComment(CommentFile, Status, back_comment, Time, StopFlag);
			if (stopFlagReturn()) {
				return false;
			}
			if (CommentFile == null) {
				sendtext("�R�����g�̃_�E�����[�h�Ɏ��s " + client.getExtraError());
				return false;
			}
		}
		sendtext("�R�����g�̕ۑ��I��");
		return true;
	}

	private boolean saveOwnerComment(NicoClient client){
		sendtext("���e�҃R�����g�̕ۑ�");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isSaveOwnerComment()) {
			if (isCommentFixFileName()) {
				if (folder.mkdir()) {
					System.out.println("Folder created: " + folder.getPath());
				}
				if (!folder.isDirectory()) {
					sendtext("���e�҃R�����g�̕ۑ���t�H���_���쐬�ł��܂���B");
					return false;
				}
				OwnerCommentFile = new File(folder, VideoID + VideoTitle + OWNER_EXT);
			} else {
				OwnerCommentFile = Setting.getOwnerCommentFile();
			}
			sendtext("���e�҃R�����g�̃_�E�����[�h�J�n��");
			if (client == null){
				sendtext("���O�C�����ĂȂ��̂ɓ��e�҃R�����g�̕ۑ��ɂȂ�܂���");
				return false;
			}
			OwnerCommentFile = client.getOwnerComment(OwnerCommentFile, Status,
					StopFlag);
			if (stopFlagReturn()) {
				return false;
			}
			if (OwnerCommentFile == null) {
				sendtext("���e�҃R�����g�̃_�E�����[�h�Ɏ��s");
				return false;
			}
		}
		sendtext("���e�҃R�����g�̕ۑ��I��");
		return true;
	}

	private Path mkTemp(String uniq){
		return Path.mkTemp(Tag + uniq);
	}

	private boolean convertComment(){
		sendtext("�R�����g�̒��ԃt�@�C���ւ̕ϊ���");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isConvertWithComment()) {
			if (Setting.isAddTimeStamp() && isCommentFixFileName()) {
				// �����̃R�����g�t�@�C���i�ߋ����O�j�����邩��
				ArrayList<String> pathlist = detectFilelistFromComment(folder);
				if (pathlist == null || pathlist.isEmpty()){
					sendtext(Tag + ": �R�����g�t�@�C���E�ߋ����O�����݂��܂���B");
					return false;
				}
				// VideoTitle �͌��������B
				if (pathlist.size() > 0) {			// 0 1.22r3e8, for NP4 comment ver 2009
					ArrayList<File> filelist = new ArrayList<File>();
					for (String path: pathlist){
						filelist.add(new File(folder, path));
					}
					CommentFile = mkTemp(TMP_COMBINED_XML);
					sendtext("�R�����g�t�@�C��������");
					if (!CombineXML.combineXML(filelist, CommentFile)){
						sendtext("�R�����g�t�@�C���������o���܂���ł����i�o�O�H�j");
						return false;
					}
					listOfCommentFile = filelist;
				} else {
					// �R�����g�t�@�C���͂ЂƂ�����������	�����ɂ͗��Ȃ� 1.22r3e8, for NP4 comment ver 2009
					File comfile = new File(folder,pathlist.get(0));
					if (isSaveComment()){			// for NP4 comment ver 2009
						ArrayList<File> filelist = new ArrayList<File>();
						filelist.add(comfile);
						CommentFile = mkTemp(TMP_COMBINED_XML);
						sendtext("�R�����g�t�@�C�����ꒆ(�V�R�����g�\��)");
						if (!CombineXML.combineXML(filelist, CommentFile)){
							sendtext("�R�����g�t�@�C��������o���܂���ł����i�o�O�H�j");
							return false;
						}
						listOfCommentFile = filelist;
					} else {
						CommentFile = comfile;
					}
				}
			}
			if (!isSaveComment()) {
				if (isCommentFixFileName()) {
					if (!Setting.isAddTimeStamp()){
						// �R�����g�t�@�C���͂ЂƂ�
						String commentfilename = detectTitleFromComment(folder);
						if(commentfilename == null){
							sendtext("�R�����g�t�@�C�����t�H���_�ɑ��݂��܂���B");
							return false;
						}
						// VideoTitle �͌��������B
						CommentFile = new File(folder, commentfilename);
						if (!CommentFile.canRead()) {
							sendtext("�R�����g�t�@�C�����ǂݍ��߂܂���B");
							return false;
						}
					} else {
						// �����ς�
					}
				} else {
					CommentFile = Setting.getCommentFile();
					if (!CommentFile.exists()) {
						sendtext("�R�����g�t�@�C�������݂��܂���B");
						return false;
					}
				}
			}
			CommentMiddleFile = convertToCommentMiddle(CommentFile, mkTemp(TMP_COMMENT));
			if (CommentMiddleFile == null){
				sendtext("�R�����g�ϊ��Ɏ��s�B�����炭���K�\���̊ԈႢ�H");
				return false;
			}
		}
		return true;
	}

	private boolean convertOwnerComment(){
		sendtext("���e�҃R�����g�̒��ԃt�@�C���ւ̕ϊ���");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isConvertWithOwnerComment()){
			if (!isSaveOwnerComment()) {
				if (isCommentFixFileName()) {
					String ownerfilename = detectTitleFromOwnerComment(folder);
					if(ownerfilename == null){
						sendtext("���e�҃R�����g�t�@�C�����t�H���_�ɑ��݂��܂���B");
						return false;
					}
					// VideoTitle �͌��������B
					OwnerCommentFile = new File(folder, ownerfilename);
					if (!OwnerCommentFile.canRead()) {
						sendtext("���e�҃R�����g�t�@�C�����ǂݍ��߂܂���B");
						return false;
					}
				} else {
					OwnerCommentFile = Setting.getOwnerCommentFile();
					if (!OwnerCommentFile.exists()) {
						sendtext("���e�҃R�����g�t�@�C�������݂��܂���B");
						return false;
					}
				}
			}
			OwnerMiddleFile = convertToCommentMiddle(OwnerCommentFile, mkTemp(TMP_OWNERCOMMENT));
			if (OwnerMiddleFile == null){
				sendtext("���e�҃R�����g�ϊ��Ɏ��s�B�����炭���K�\���̊ԈႢ�H");
				return false;
			}
		}
		return true;
	}

	private void deleteCommentFile(){
		if (CommentFile.delete()) {
			System.out.println("Deleted: " + CommentFile.getPath());
		}
		deleteList(listOfCommentFile);
		// OwnerCommentFile.delete();
		if (OwnerCommentFile != null && OwnerCommentFile.delete()) {
			System.out.println("Deleted: " + OwnerCommentFile.getPath());
		}
	}

	private File convertToCommentMiddle(File commentfile, File middlefile) {
		if (!ConvertToVideoHook.convert(
				commentfile, middlefile,
				Setting.getNG_ID(), Setting.getNG_Word())){
			return null;
		}
		return middlefile;
	}

	private boolean convertVideo() throws IOException {
		sendtext("����̕ϊ����J�n");
		Stopwatch.start();
		/*�r�f�I���̊m��*/
		File folder = Setting.getConvFixFileNameFolder();
		if (!chekAspectVhookOption(VideoFile, wayOfVhook)){
			return false;
		}
		if (Setting.isConvFixFileName()) {
			if (folder.mkdir()) {
				System.out.println("Created folder: " + folder.getPath());
			}
			if (!folder.isDirectory()) {
				sendtext("�ϊ���̕ۑ���t�H���_���쐬�ł��܂���B");
				return false;
			}
			String conv_name = VideoTitle;
			if (conv_name == null){
				conv_name = "";
			}
			if (!Setting.isNotAddVideoID_Conv()) {//�t������Ȃ�
				conv_name = VideoID + conv_name;
			}
			if (conv_name.isEmpty()) {
				sendtext("�ϊ���̃r�f�I�t�@�C�������m��ł��܂���B");
				return false;
			}

			if (Setting.isAddOption_ConvVideoFile()){
				byte[] dirName = new File(folder, conv_name)
					.getAbsolutePath().getBytes("Shift_JIS");
				// �t�H���_������������ꍇ
				if (dirName.length > (255-12)){
					conv_name = VideoID;
				}
				conv_name = conv_name.trim();	// In Windows API, cant make dir as " ABC" nor "ABC "
				folder = new File(folder, conv_name);
				if (folder.mkdir()) {
					System.out.println("Created folder: " + folder.getPath());
				}
				if (!folder.isDirectory()) {
					sendtext("����(FFmpeg�ݒ薼)�t�@�C���̕ۑ���t�H���_���쐬�ł��܂���B");
					return false;
				}
//				if (!chekAspectVhookOption(VideoFile, wayOfVhook)){
//					return false;
//				}
				conv_name = MainOption + InOption + OutOption;
				if (!getFFmpegVfOption().isEmpty()){
					conv_name = VFILTER_FLAG + " " + getFFmpegVfOption() + conv_name;
				}
				conv_name = getFFmpegOptionName() + safeAsciiFileName(conv_name);
				dirName = new File(folder, conv_name).getAbsolutePath().getBytes("Shift_JIS");
				// �t�@�C��������������ꍇ
				if (dirName.length > (255 - 3)){
					int len = conv_name.length() - (dirName.length - (255 - 3));
					if (len < 1){
						sendtext("�쐬����r�f�I�t�@�C�������������܂��B");
						return false;
					}
					conv_name = conv_name.substring(0, len);
				}
				conv_name = conv_name.trim();
			}
			ConvertedVideoFile = new File(folder, conv_name + ExtOption);
		} else {
			String filename = Setting.getConvertedVideoFile().getPath();
			if (!filename.endsWith(ExtOption)) {
				filename = filename.substring(0, filename.lastIndexOf('.'))
						+ ExtOption;
				ConvertedVideoFile = new File(filename);
			} else {
				ConvertedVideoFile = Setting.getConvertedVideoFile();
			}
//			if (!chekAspectVhookOption(VideoFile, wayOfVhook)){
//				return false;
//			}
		}
		if (ConvertedVideoFile.getAbsolutePath().equals(VideoFile.getAbsolutePath())){
			sendtext("�ϊ���̃t�@�C�������ϊ��O�Ɠ����ł�");
			return false;
		}
		int code = converting_video(CommentMiddleFile, OwnerMiddleFile);
		Stopwatch.stop();
		if (code == 0) {
			sendtext("�ϊ�������ɏI�����܂����B");
			System.out.println(ffmpeg.getLastFrame());
			return true;
		} else if (code == CODE_CONVERTING_ABORTED) { /*���f*/

		} else {
			sendtext("�ϊ��G���[�F" + ffmpeg.getLastError());
		}
		return false;
	}

	private static String safeAsciiFileName(String str) {
		//Windows�t�@�C���V�X�e���ň�����Ascii�������
		str = str.replace('/', '_')
			.replace('\\', '_')
			.replace('?', '_')
			.replace('*', '_')
			.replace(':', ';')		//	:(colon) to ;(semi colon)
			.replace('|', '_')
			.replace('\"', '\'')
			.replace('<', '(')
			.replace('>', ')')
//			.replace('.', '�D')		// .(dot) is let there
			.replaceAll(" +", " ")
			.trim();
		return str;
	}

	@Override
	public void run() {
		try {
			Stopwatch.clear();
			Stopwatch.start();
			if (!checkOK()) {
				return;
			}
			NicoClient client = getNicoClient();
			if (client != null){
				if (!client.isLoggedIn()){
					return;
				}
				if (!client.getVideoInfo(Tag, WatchInfo, Time)) {
					sendtext(Tag + "�̏��̎擾�Ɏ��s " + client.getExtraError());
					return;
				}
				if (stopFlagReturn()) {
					return;
				}
				VideoTitle = client.getVideoTitle();
				sendtext(Tag + "�̏��̎擾�ɐ���");
			}

			Stopwatch.show();
			if (!saveVideo(client) || stopFlagReturn()) {
				return;
			}

			Stopwatch.show();
			if (!saveComment(client) || stopFlagReturn()){
				return;
			}

			Stopwatch.show();
			if (!saveOwnerComment(client) || stopFlagReturn()) {
				return;
			}

			Stopwatch.show();
			System.out.println("�ϊ��O���ԁ@" + Stopwatch.formatElapsedTime());

			if (!isSaveConverted()) {
				sendtext("����E�R�����g��ۑ����A�ϊ��͍s���܂���ł����B");
				return;
			}

			Stopwatch.show();
			if (!convertComment() || stopFlagReturn()) {
				return;
			}

			Stopwatch.show();
			if (!convertOwnerComment() || stopFlagReturn()){
				return;
			}

			Stopwatch.show();
			if (convertVideo()) {
				if (isDeleteCommentAfterConverting()
					&& CommentFile != null) {
					deleteCommentFile();
					// deleteList(listOfCommentFile);
					// OwnerCommentFile.delete();
				}
				if (isDeleteVideoAfterConverting()
					&& VideoFile != null) {
					if (VideoFile.delete()) {
						System.out.println("Deleted: " + VideoFile.getPath());
					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			StopFlag.finished();
			if (CommentMiddleFile != null) {
				if (CommentMiddleFile.delete()) {
					System.out.println("Deleted: " + CommentMiddleFile.getPath());
				}
			}
			if (OwnerMiddleFile != null){
				if (OwnerMiddleFile.delete()) {
					System.out.println("Deleted: " + OwnerMiddleFile.getPath());
				}
			}
			Stopwatch.show();
			Stopwatch.stop();
			System.out.println("�ϊ����ԁ@" + Stopwatch.formatLatency());
			System.out.println("LastStatus: " + Status.getText());
			System.out.println("VideoInfo: " + MovieInfo.getText());
		}
	}

	private void deleteList(ArrayList<File> list){
		if (list== null)	{
			return;
		}
		boolean b = true;
		for (File file : list){
			b = file.delete() && b;
		}
		if (!b){
			System.out.println("Can't delete list of all Comment.");
		}
	}

	/**
	 * CWS�Ȃ�FWS�ɕϊ�����<br/>
	 * ���̌�A�A�X�y�N�g��𔻒肵Vhook��I���A�I�v�V������ǂݍ��ݐݒ肷��
	 * @param video : File
	 * @param way : int  1 or 2
	 * Output videoAspect : Aspect
	 * Output VideoFile : File
	 * Output selectedVhook : File  vhook.exe
	 * OUTPUT ExtOption, MainOption, InOption, OutOption
	 */
	private boolean chekAspectVhookOption(File video, int way){
		fwsFile = null;
		try {
			fwsFile = Cws2Fws.createFws(video);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (fwsFile != null){
			VideoFile = fwsFile;
			video = fwsFile;
		}
		videoAspect = ffmpeg.getAspect(video);
		String str;
		if (videoAspect == null){
			str = "Analize Error   ";
			videoAspect = Aspect.NORMAL;
		} else {
			str = videoAspect.explain() + "  ";
		}
		if (way == 1){
			if (VhookNormal == null){
				if (!videoAspect.isWide()){
					str = "��" + str;
				}
				videoAspect = Aspect.WIDE;
			} else {
				if (videoAspect.isWide()){
					str = "��" + str;
				}
				videoAspect = Aspect.NORMAL;
			}
		}
		if (videoAspect.isWide()){
			selectedVhook = VhookWide;
			MovieInfo.setText("�g��Vhook ���C�h " + str);
		} else {
			selectedVhook = VhookNormal;
			MovieInfo.setText("�g��Vhook �]�� " + str);
		}
		if (!detectOption(videoAspect.isWide())){
			sendtext("�ϊ��I�v�V�����t�@�C���̓ǂݍ��݂Ɏ��s���܂����B");
			return false;
		}
		return true;
	}

	private static final int CODE_CONVERTING_ABORTED = 100;

	private int converting_video(File comment, File owner_comment) {
		int code = -1;
//		if (!chekAspectVhookOption(VideoFile, wayOfVhook)) {
//			return code;
//		}
		/*
		 * ffmpeg.exe -y mainoption inoption -i infile outoptiont [vhookOption] outfile
		 */
		ffmpeg.setCmd("-y ");
		ffmpeg.addCmd(MainOption);
		ffmpeg.addCmd(" ");
		ffmpeg.addCmd(InOption);
		ffmpeg.addCmd(" -i ");
		ffmpeg.addFile(VideoFile);
		ffmpeg.addCmd(" ");
		ffmpeg.addCmd(OutOption);
		if (!Setting.isVhookDisabled()) {
			if(!addVhookSetting(ffmpeg, selectedVhook, videoAspect.isWide())){
				return -1;
			}
		} else if (!getFFmpegVfOption().isEmpty()){
			ffmpeg.addCmd(" -vfilters ");
			ffmpeg.addCmd(getFFmpegVfOption());
		}
		ffmpeg.addCmd(" ");
		ffmpeg.addFile(ConvertedVideoFile);

		System.out.println("arg:" + ffmpeg.getCmd());
		code = ffmpeg.exec(Status, CODE_CONVERTING_ABORTED, StopFlag, Stopwatch);
		if (fwsFile != null) {
			// fwsFile.delete();	// For DEBUG
		}
		return code;
	}
/*
	public File nmm2avi(File fwstmp) throws IOException {
		int code = -1;
		File tmpdir = new File("tmpdir");
		tmpdir.mkdir();
		if (!tmpdir.isDirectory()){
			sendtext("�ꎞ�t�@�C���̕ۑ���t�H���_���쐬�ł��܂���B");
			throw new IOException("");
		}
		File [] list = tmpdir.listFiles();
		for (File file: list){
			file.delete();
		}
		File videoavi = new File("fws_tmp.avi");
		if(videoavi.exists()){
			videoavi.delete();
		}
		File avi = new File("fws_out.avi");
		if(avi.exists()){
			avi.delete();
		}
*/		/*
		 * ffmpeg.exe -r 25 -y -i fws_tmp.swf -an -vcodec copy -f image2 %03d.jpg
		 */
/*		ffmpeg.setCmd("-xerror -y -i ");
		ffmpeg.addFile(fwstmp);
//		ffmpeg.addCmd(" -an -vcodec copy -f image2 -vbsf mjpeg2jpeg ");
		ffmpeg.addCmd(" -an -f image2 ");
		ffmpeg.addFile(new File(tmpdir, "%08d.jpg"));
		System.out.println("nmm2avi:" + ffmpeg.getCmd());
		sendtext("NMM���悩��ꎞ�t�@�C���𒊏o��");
		code = ffmpeg.exec(Status, CODE_CONVERTING_ABORTED, StopFlag);
		if (code != 0){
			if (code != CODE_CONVERTING_ABORTED) { // ���f
				sendtext("�ϊ��G���[�F" + ffmpeg.getLastError());
			}
			throw new IOException("");
		}
*/		/*
		 * JPEG�t�@�C����AVI�`���ɍ���
		 * ffmpeg.exe -r 1/4 -y -i %03d.jpg -an -vcodec huffyuv -f avi huffjpg.avi
		 */
/*		ffmpeg.setCmd("-xerror -r 1/4 -y -i ");
		ffmpeg.addFile(new File(tmpdir, "%08d.jpg"));
		ffmpeg.addCmd(" -an -vcodec huffyuv -f avi ");
		ffmpeg.addFile(videoavi);
		System.out.println("nmm2avi:" + ffmpeg.getCmd());
		sendtext("NMM�����AVI�`���ɕϊ���");
		code = ffmpeg.exec(Status, CODE_CONVERTING_ABORTED, StopFlag);
		if (code != 0){
			if (code != CODE_CONVERTING_ABORTED) {
				sendtext("�ϊ��G���[�F" + ffmpeg.getLastError());
			}
			throw new IOException("");
		}
*/		/*
		 * ����������
		 * ffmpeg.exe -y -i fws_tmp.swf -itsoffset 1.0 -i avi4.avi
		 *  -vcodec libxvid -acodec libmp3lame -ab 128k -ar 44100 -ac 2 fwsmp4.avi
		 */
/*		ffmpeg.setCmd("-xerror -y -i ");
		ffmpeg.addFile(fwstmp);
		ffmpeg.addCmd(" -i ");
		ffmpeg.addFile(videoavi);
		ffmpeg.addCmd(" -acodec copy -vcodec copy ");
		ffmpeg.addFile(avi);
		System.out.println("nmm2avi:" + ffmpeg.getCmd());
		sendtext("AVI����ɉ�����������");
		code = ffmpeg.exec(Status, CODE_CONVERTING_ABORTED, StopFlag);
		if (code != 0){
			if (code != CODE_CONVERTING_ABORTED) {
				sendtext("�ϊ��G���[�F" + ffmpeg.getLastError());
			}
			throw new IOException("");
		}
		return avi;
	}
*/
	private boolean addVhookSetting(FFmpeg ffmpeg, File vhookExe, boolean isWide) {
		try {
			ffmpeg.addCmd(" -vfilters \"");
			if (!getFFmpegVfOption().isEmpty()){
				ffmpeg.addCmd(getFFmpegVfOption());
				ffmpeg.addCmd(",");
			}
			ffmpeg.addCmd("vhext=");
			ffmpeg.addFile(vhookExe);
			ffmpeg.addCmd("|");
			if(CommentMiddleFile!=null){
				ffmpeg.addCmd("--data-user:");
				ffmpeg.addCmd(URLEncoder.encode(
					Path.toUnixPath(CommentMiddleFile), "Shift_JIS"));
				ffmpeg.addCmd("|");
			}
			if(OwnerMiddleFile!=null){
				ffmpeg.addCmd("--data-owner:");
				ffmpeg.addCmd(URLEncoder.encode(
					Path.toUnixPath(OwnerMiddleFile), "Shift_JIS"));
				ffmpeg.addCmd("|");
				ffmpeg.addCmd("--show-owner:" + NicoClient.STR_OWNER_COMMENT + "|");
			}
			ffmpeg.addCmd("--font:");
			ffmpeg.addCmd(URLEncoder.encode(
				Path.toUnixPath(Setting.getFontPath()), "Shift_JIS"));
			ffmpeg.addCmd("|--font-index:");
			ffmpeg.addCmd(Setting.getFontIndex());
			ffmpeg.addCmd("|--show-user:");
			ffmpeg.addCmd(Setting.getVideoShowNum());
			ffmpeg.addCmd("|--shadow:" + Setting.getShadowIndex());
			ffmpeg.addCmd("|");
			if (Setting.isVhook_ShowConvertingVideo()) {
				ffmpeg.addCmd("--enable-show-video|");
			}
			if (Setting.isFixFontSize()) {
				ffmpeg.addCmd("--enable-fix-font-size|");
			}
			if (Setting.isOpaqueComment()) {
				ffmpeg.addCmd("--enable-opaque-comment|");
			}
			if (isWide){
				ffmpeg.addCmd("--nico-width-wide|");
			}
			ffmpeg.addCmd("\"");
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
	}

	/*
	private static void addArrayToList(ArrayList<String> list,String array[]){
	for(int i=0;i<array.length;i++){
	list.add(array[i]);
	}
	}
	private static String escape(String str){
	byte[] buff = null;
	try {
	buff = str.getBytes("Shift_JIS");
	} catch (UnsupportedEncodingException e) {
	e.printStackTrace();
	}
	int cnt = 0;
	for(int i=0;i<buff.length;i++){
	if(buff[i] == '\\' || buff[i] == '{' || buff[i] == '}'){
	cnt++;
	}
	cnt++;
	}
	byte[] obuff = new byte[cnt];
	cnt = 0;
	for(int i=0;i<buff.length;i++){
	if(buff[i] == '\\' || buff[i] == '{' || buff[i] == '}'){
	obuff[cnt] = '\\';
	cnt++;
	}
	obuff[cnt] = buff[i];
	cnt++;
	}
	try {
	String out = new String(obuff,"Shift_JIS");
	return out;
	} catch (UnsupportedEncodingException e) {
	e.printStackTrace();
	}
	return "";
	}
	 */
	public boolean isConverted() {
		return StopFlag.isConverted();
	}

	private boolean stopFlagReturn() {
		if (StopFlag.needStop()) {
			sendtext("���~���܂����B");
			return true;
		}
		return false;
	}

	public ConvertStopFlag getStopFlag() {
		return this.StopFlag;
	}

	private String ExtOption;

	private String InOption;

	private String OutOption;

	private String MainOption;

	private String ffmpegOptionName = "���ړ���";

	private String ffmpegVfOption = "";

	boolean detectOption(boolean isWide) {
		File option_file = null;
		if (!isWide) {
			option_file = Setting.getOptionFile();
		} else {
			option_file = Setting.getWideOptionFile();
		}
		if (option_file != null) {
			try {
				Properties prop = new Properties();
				prop.loadFromXML(new FileInputStream(option_file));
				ExtOption = prop.getProperty("EXT");
				InOption = prop.getProperty("IN");
				OutOption = prop.getProperty("OUT");
				MainOption = prop.getProperty("MAIN");
				if (ExtOption == null || InOption == null || OutOption == null
						|| MainOption == null) {
					return false;
				}
				ffmpegOptionName = option_file.getName().replace(".xml", "");
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}
		} else {
			if (!isWide){
				ExtOption = Setting.getCmdLineOptionExt();
				InOption = Setting.getCmdLineOptionIn();
				OutOption = Setting.getCmdLineOptionOut();
				MainOption = Setting.getCmdLineOptionMain();
			} else {
				ExtOption = Setting.getWideCmdLineOptionExt();
				InOption = Setting.getWideCmdLineOptionIn();
				OutOption = Setting.getWideCmdLineOptionOut();
				MainOption = Setting.getWideCmdLineOptionMain();
			}
		}
		//�I�v�V�����Ɋg���q���܂�ł��܂����ꍇ�ɂ��Ή���
		if(ExtOption != null && !ExtOption.startsWith(".")){
			ExtOption = "."+ExtOption;
		}
		ffmpegVfOption = getvfOption();
		return true;
	}

	private String getvfOption() {
		String vfIn, vfOut, vfMain;
		vfIn = getvfOption(InOption);
		InOption = deletevfOption(InOption, vfIn);
		vfOut = getvfOption(OutOption);
		OutOption = deletevfOption(OutOption, vfOut);
		vfMain = getvfOption(MainOption);
		MainOption = deletevfOption(MainOption, vfMain);
		if (vfIn.isEmpty()){
			vfIn = vfMain;
		} else if (!vfMain.isEmpty()){
			vfIn += "," + vfMain;
		}
		if (vfIn.isEmpty()){
			vfIn = vfOut;
		} else if (!vfOut.isEmpty()){
			vfIn += "," + vfOut;
		}
		return vfIn;
	}
	private static final String VFILTER_FLAG = "-vfilters";
	private String getvfOption(String option){
		if (option == null){
			return "";
		}
		int index;
		if ((index = option.indexOf(VFILTER_FLAG)) < 0){
			return "";
		}
		option = option.substring(index + VFILTER_FLAG.length());
		option = option.trim();
		if ((index = option.indexOf(" ")) < 0){
			return option;
		}
		option = option.substring(0, index);
		return option;
	}
	private String deletevfOption(String option, String vfoption){
		if (option == null){
			return "";
		}
		return option.replace(VFILTER_FLAG,"").replace(vfoption, "")
			.replaceAll(" +", " ");
	}
	public String getInOption(){
			return InOption;
	}
	public String getFFmpegOptionName() {
		return ffmpegOptionName;
	}
	public String getFFmpegVfOption() {
		return ffmpegVfOption;
	}
	/**
	 *
	 * @author orz
	 *
	 */
	private static class VideoIDFilter implements FilenameFilter {
		private final String VideoTag;
		public VideoIDFilter(String videoTag){
			VideoTag = videoTag;
		}
		@Override
		public boolean accept(File dir, String name) {
			if (name.indexOf(VideoTag) >= 0){
				return true;
			}
			return false;
		}
	}

	String detectTitleFromVideo(File dir){
		if (dir == null){
			return null;
		}
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if(list == null){
			return null;
		}
		for (int i = 0; i < list.length; i++) {
			if (list[i].startsWith(VideoID)) {
				String path = list[i];
				if(!path.endsWith(".flv")){
					OtherVideo = path;
					continue;
				}
				if (VideoTitle != null){
					return path;
				}
				VideoTitle = getTitleFromPath(path, VideoID);
				return path;
			}
		}
		return null;
	}

	private String detectTitleFromComment(File dir){
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if(list == null){ return null; }
		for (int i = 0; i < list.length; i++) {
			String path = list[i];
			if (!path.endsWith(".xml") || path.endsWith(OWNER_EXT)){
				continue;
			}
			if (VideoTitle == null){
				VideoTitle = getTitleFromPath(path, VideoID);
				int index = VideoTitle.lastIndexOf("[");
					//�ߋ����O��[YYYY/MM/DD_HH:MM:SS]���Ō�ɕt��
				if (index >= 0){
					VideoTitle = VideoTitle.substring(0, index);
				}
			}
			return path;
		}
		return null;
	}

	private static final String TCOMMENT_EXT =".txml";
	private String detectTitleFromOwnerComment(File dir){
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if(list == null){ return null; }
		for (int i = 0; i < list.length; i++) {
			String path = list[i];
			String ext;
			if (path.endsWith(OWNER_EXT)){
				ext = OWNER_EXT;
			} else if (path.endsWith(TCOMMENT_EXT)) {
				ext = TCOMMENT_EXT;
			} else {
				continue;
			}
			if (VideoTitle == null){
				VideoTitle = getTitleFromPath(path, VideoID)
					.substring(0, VideoTitle.lastIndexOf(ext));
			}
			return path;
		}
		return null;
	}

	private ArrayList<String> detectFilelistFromComment(File dir){
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if (list == null) { return null; }
		ArrayList<String> filelist = new ArrayList<String>();
		for (String path : list){
			if (!path.endsWith(".xml") || path.endsWith(OWNER_EXT)){
				continue;
			}
			if (VideoTitle == null){
				VideoTitle = getTitleFromPath(path, VideoID);
				int index = VideoTitle.lastIndexOf("[");
					//�ߋ����O��[YYYY/MM/DD_HH:MM:SS]���Ō�ɕt��
				if (index >= 0){
					VideoTitle = VideoTitle.substring(0, index);
				}
			}
			filelist.add(path);
		}
		return filelist;
	}

	/*
	 * 	���e�҃R�����g�Ɋւ���t�@�C�����^�O�Ɗg���q������
	 * 		�@ NicoBrowser�g��1.4.4�̏ꍇ
	 * 			���[�U�R�����g = VideiID + VideoTitle + ".xml"
	 * 			���e�҃R�����g = VideoID + VideoTitle + ".txml"
	 * 		�A����̂�����΂�
	 * 			���[�U�R�����g = VideoID + VideoTitle + ".xml"
	 * 			�ߋ����O       = VideoID + VideoTitle + "[YYYY�^MM�^DD_HH�Fmm�Fss].xml"
	 * 			���e�҃R�����g = VideoID + VideoTitle + "{Owner].xml"
	 * 		�BNNDD�Ȃ�
	 * 			���[�U�R�����g = VideoTitle + VideoID + ".xml"
	 * 			���e�҃R�����g = VideoTitle + VideoID + "{Owner].xml"
	 * 		�CNicoPlayer�Ȃ�
	 * 			���[�U�R�����g = VideoTitle + "(" + Tag + ")" + ".xml"
	 * 			�ߋ����O       = VideoTitie + "(" + Tag + ")[" + YYYY�NMM��DD��HH��MM��SS�b + "}.xml"
	 * 			���e�҃R�����g = VideoTitle + "(" + Tag + "){Owner].xml"
	 *
	 *
	 */

	/*
	 * videoID�̈ʒu�͖��֌W�ɍ폜
	 * �g���q������΂��̑O�܂�
	 */
	private String getTitleFromPath(String path, String videoID){
		if (path.indexOf(videoID) >= 0){
			path = path.replace(videoID, "");	// videoID�̈ʒu�͖��֌W�ɍ폜
		}
		// �g���q������΂��̑O�܂�
		if (path.lastIndexOf(".") > path.lastIndexOf(File.separator)){
			path = path.substring(0, path.lastIndexOf("."));
		}
		return path;
	}

	/*
	 * lastChar �� videoID �����ɂ����
	 * �Ō�� lastChar �̑O�܂łɏk�߂�
	 * lastChar�̓^�C�g���Ƀ_�u���Ċ܂܂�Ă悢��
	 * �^�C�g����ɏ��Ȃ��Ƃ�1���邱�Ƃ͊m���łȂ��ƃ_��
	 */
	/*
	private String getTitileFromPath(String path, String videoID,
			String lastChar){
		if (lastChar != null
				&& path.lastIndexOf(lastChar) > path.indexOf(videoID)){
			path = path.substring(0, path.lastIndexOf(lastChar));
		}
		return getTitleFromPath(path, videoID);
	}
	*/

}