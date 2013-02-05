/*
 * util.c
 *
 *  Created on: 2011/11/14
 *      Author: orz
 */
#include <stdio.h>
#include "uniutil.h"
#include "../mydef.h"
#include "unitable.h"
#include "../nicodef.h"

int isMatchKanji(Uint16* u,Uint16* kanji){
	if(kanji==NULL)
		return FALSE;
	while(*kanji!='\0'){
		if(*kanji<=*u && *u<=*(kanji+1)){
			return TRUE;
		}
		kanji += 2;
	}
	return FALSE;
}

int isAscii(Uint16* u){
	return ((0x0000 < *u && *u < 0x0080) || *u==0x00a0);
}

int getDetailType(int u){
	return UNITABLE[u];
}

int isGlyphExist(DATA* data,int fonttype,Uint16 u){
	TTF_Font* font = data->CAfont[fonttype][CMD_FONT_DEF];
	return (font!=NULL && TTF_GlyphIsProvided(font,u));
}

int getGlyphExist(DATA* data,Uint16 u){
	int f;
	for(f=0;f<CA_FONT_MAX;f++){
		if(isGlyphExist(data,f,u))
			return f;
	}
	return UNDEFINED_FONT;
}

/*
int isZeroWidth(Uint16* u){
	if(0x2029<=*u && *u<=0x202f){
		return TRUE;
	}
	if(*u==0x200b){
		return TRUE;
	}
	return FALSE;
}
*/
int isZeroWidth(Uint16* u){
	return (getDetailType(*u)==ZERO_WIDTH_CHAR);
}

int getFontType2(Uint16* u,int basefont,DATA* data){
	if(u==NULL || *u == '\0'){
		return NULL_FONT;
	}
	if(*u==0x0020||*u==0x00a0){	//Ascii space -> fix fontsize w,h
		return ARIAL_FONT|0x0000|(*u & 0x00f0);	//0020-> 0023 00a0-> 00a3
	}
	if(0x2000<=*u && *u<=0x200f){	//Various width Space -> fix fontsize w,h
		return basefont|0x2000|(*u & 0x000f)<<4;	//2001->2010 200a->20a0;
	}
	if(*u==0x3000){	//全角空白->fix fontsize w,h
		return basefont|0x3000;		//SIMSUN 3001 GULIM 3002
	}
	switch(getDetailType(*u)){
		case STRONG_SIMSUN_CHAR:
		case WEAK_SIMSUN_CHAR:
		case SIMSUN_NOT_CHANGE_CHAR:
		//case UNDEF_OR_SIMSUN:	//最終的にはXP,Vista,Win7共通時にSIMSUNに
			return SIMSUN_FONT;
		case MINGLIU_CHAR:	//明朝変化なし
			return MINGLIU_FONT;
		case N_MINCHO_CHAR:	//明朝変化なし simsun or ngulim
			return N_MINCHO_FONT;
		case GULIM_CHAR:
			return GULIM_FONT;
		case GOTHIC_CHAR:	//ゴシック保護
		case GOTHIC_NOT_PROTECT:	//保護なしゴシック
			return GOTHIC_FONT;
		case ARIAL_CHAR:
			return ARIAL_FONT;
		case GEORGIA_CHAR:	//use special
			return GEORGIA_FONT;
//		case UI_GOTHIC_CHAR:	//use special
//			return UI_GOTHIC_FONT;
		case DEVANAGARI_CHAR:	//use special
			return DEVANAGARI;
		case TAHOMA_CHAR:
			return TAHOMA_FONT;
		case ESTRANGELO_EDESSA:
			return ESTRANGELO_EDESSA_FONT;
		default:
			//include UNDEFINED_CHAR
			if(isGlyphExist(data,basefont,*u))
				return basefont;
			else
				return getGlyphExist(data,*u);
	}
}

int getFontType(Uint16* u,int basefont,DATA* data){
	if(isMatchExtra(u,data->extra_change))
		return EXTRA_FONT;
	else
		return getFontType2(u,basefont,data);
}

int getFirstFont(Uint16* u,int basefont){
	if(u==NULL || *u == '\0'){
		return basefont;
	}
	int foundBase = FALSE;
	while(*u!='\0'){
		if(!isAscii(u))
			foundBase = TRUE;
		else if(foundBase)
			return basefont;
		switch (getDetailType(*u)) {
			case STRONG_SIMSUN_CHAR:
				return SIMSUN_FONT;
			case GULIM_CHAR:
				return GULIM_FONT;
			case GOTHIC_CHAR:
				return GOTHIC_FONT;
			case WEAK_SIMSUN_CHAR:
				basefont = SIMSUN_FONT;
				break;
			default:
				break;
		}
		u++;
	}
	return basefont;
}

/*
Uint16 replaceSpace(Uint16 u){
	if(u == 0x02cb){
		return 0x02cb;
	}else{
		return u;
	}
}
*/

void removeZeroWidth(Uint16* str,int len){
	int i;
	Uint16* dst = str;
	for(i=0;i<len;i++){
		if(!isZeroWidth(str)){
			*dst++ = *str++;
		}else{
			str++;
		}
	}
	return;
}

int convUint16Pair(const char** unicodep,Uint16* up);
int getUint16(const char** unicodep);

int convUint16(const char* unicode, Uint16** out){
	int len = 256;
	Uint16* p = malloc(sizeof(Uint16)*2*len);
	if(p==NULL){
		*out = NULL;
		return 0;
	}
	int i;
	int n = 0;
	int uc;
	const char* start = unicode;
	for(i=0;i<len;i+=2){
		if(convUint16Pair(&start,&p[i])){
			continue;
		}
		uc = getUint16(&start);
		if(uc != UNDEFINED_FONT){
			uc &= 0xffff;
			p[i] = uc;
			p[i+1] = uc;
			continue;
		}
		break;
	}
	n = i;
	*out = malloc(sizeof(Uint16)*(n+2));
	if(*out==NULL){
		free(p);
		return 0;
	}
	for(i=0;i<n;i++){
		(*out)[i] = p[i];
	}
	(*out)[n] = '\0';
	(*out)[n+1] = '\0';
	free(p);
	return n;
}

int convUint16Pair(const char** unicodep,Uint16* up){
	int u0, u1;
	const char* ip = *unicodep;
	u0 = getUint16(&ip);
	if(u0 == UNDEFINED_FONT){
		return FALSE;
	}
	if(*ip!='-'){
		return FALSE;
	}
	ip++;
	u1 = getUint16(&ip);
	if(u1 == UNDEFINED_FONT){
		return FALSE;
	}
	up[0] = (Uint16)u0;
	up[1] = (Uint16)u1;
	*unicodep = ip;
	return TRUE;
}

int getUint16(const char** unicodep){
	int uc;
	char* endp;
	uc = strtoul(*unicodep,&endp,16);
	if(uc <=0 || 0xffff< uc){
		return UNDEFINED_FONT;
	}
	*unicodep = endp;
	return uc;
}

int uint16len(Uint16* u){
	if(u==NULL)
		return 0;
	int l=0;
	while(*u++!='\0')
		l++;
	return l;
}

int isKanjiWidth(Uint16* u){
	return isMatchKanji(u,(Uint16*)&KANJI_WIDTH[0]);
}
