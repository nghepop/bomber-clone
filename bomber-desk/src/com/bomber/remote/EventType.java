package com.bomber.remote;

public class EventType {
	public static final short CREATE = 0;
	public static final short DESTROY = 1;
	public static final short MOVE = 2;
	public static final short STOP = 3;
	public static final short START = 4;
	public static final short SYNC = 5;
	public static final short SELECTED_TEAM = 6;

	
	// short cont�m o remoteid da liga��o que corresponde ao mRemoteId do Player
	// string cont�m a raz�o
	public static final short DISCONNECTED = 7;
	
	// Indica ao cliente para se ligar a outro
	// string cont�m os dados da liga��o p.e "ip:porto"
	public static final short CONNECT_TO = 8;
	public static final short PING = 9;
	public static final short PONG = 10;
	public static final short SET_ID = 11;
	public static final short LOCAL_SERVER_PORT = 12;
	public static final short STOP_LOCAL_SERVER = 13;
	
	// Indica os segundos at� o jogo iniciar
	// o valor dos segundos vem no valShort
	public static final short COUNTDOWN = 13;
	public static final short RANDOM_SEED = 14;
}