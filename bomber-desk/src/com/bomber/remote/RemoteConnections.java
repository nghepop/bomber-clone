package com.bomber.remote;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Random;

import com.bomber.Game;
import com.bomber.Settings;
import com.bomber.common.ObjectFactory;
import com.bomber.common.Strings;
import com.bomber.common.Utils;
import com.bomber.gamestates.GameStateLoadingPVP;
import com.bomber.gamestates.GameStateServerConnectionError;
import com.bomber.remote.bluetooth.BluetoothLocalServer;
import com.bomber.remote.tcp.TCPLocalServer;
import com.bomber.remote.udp.UDPLocalServer;

public class RemoteConnections {

	public static GameServer mGameServer = null;
	private ArrayList<Connection> mPlayers;
	private LocalServer mLocalServer = null;

	private boolean mAcceptingConnections = false;

	public static Game mGame = null;
	public static short mLocalID = 0;
	public static short mProtocolInUse = -1;

	public static boolean mIsGameServer = false;
	public MessageContainer mRecvMessages = null;
	public Message mMessageToSend = new Message();

	public RemoteConnections(boolean _isGameServer, short _protocol) {
		mProtocolInUse = _protocol;
		mGame = null;
		mLocalID = 0;

		mRecvMessages = new MessageContainer();
		mPlayers = new ArrayList<Connection>();

		mIsGameServer = _isGameServer;

		mGameServer = null;
		if (mIsGameServer)
			mGameServer = new GameServer(null, mPlayers);
	}

	public synchronized void removePlayer(short _id)
	{
		// Se for o servidor ent�o retorna
		if (!mGameServer.isConnected() || !mIsGameServer)
			return;

		int idx = 0;
		for (; idx < mPlayers.size(); idx++)
		{
			if (mPlayers.get(idx).mRemoteID == _id)
			{
				mGameServer.releaseId(_id);
				break;
			}
		}

		if (idx == mPlayers.size())
			throw new InvalidParameterException("O id n�o existe");

		mPlayers.remove(idx);

		mLocalServer.nAdded--;
	}

	public boolean connectedToServer()
	{
		return mGameServer.isConnected();
	}

	/**
	 * 
	 * @param _protocol
	 *            Protocolo a usar.
	 * @param _connectionString
	 *            Dados da liga��o. Varia consoante o protocolo. TCP p.e:
	 *            "localhost:50087"
	 * @return <b>true</b> caso a conex�o seja bem sucedida. <b>false</b> caso a
	 *         liga��o falhe.
	 */
	public boolean connectToGameServer(short _protocol, String _connectionString) throws IOException
	{
		// Cria uma liga��o ao servidor
		Connection tmpConn = ObjectFactory.CreateConnection.Create(_protocol, _connectionString, mRecvMessages);

		if (tmpConn == null)
			return false;

		// Envia mensagem ao servidor a indicar a porta local onde estamos a
		// aceitar liga��es por parte de outros jogadores
		mMessageToSend.messageType = MessageType.CONNECTION;
		mMessageToSend.eventType = EventType.LOCAL_SERVER_PORT;
		if (_protocol != Protocols.BLUETOOTH)
			mMessageToSend.valInt = mLocalServer.getLocalPort();
		else
			mMessageToSend.valInt = 1;
		tmpConn.sendMessage(mMessageToSend);


		tmpConn.setDaemon(true);
		tmpConn.start();

		// Cria uma inst�ncia do objecto que representa o servidor
		mGameServer = new GameServer(tmpConn, mPlayers);

		return true;
	}

	/**
	 * Fun��o chamada quando � recebida uma {@link Message} com um
	 * {@link MessageType.CONNECTION} e um {@link EventType.CONNECT_TO} do
	 * servidor. � efectuada uma liga��o a um outro player com um endere�o
	 * constante da mensagem.
	 * 
	 * @param _protocol
	 *            Protocolo a usar.
	 * @param _connectionString
	 *            Dados da liga��o. Varia consoante o protocolo. TCP p.e:
	 *            "localhost:50087"
	 * @return <b>true</b> caso a conex�o seja bem sucedida. <b>false</b> caso a
	 *         liga��o falhe.
	 * @throws IOException
	 */
	public boolean connectToPlayer(short _protocol, String _connectionString) throws IOException
	{
		Utils.LOG("A tentar ligar ao cliente: " + _connectionString);

		Connection tmpConn = ObjectFactory.CreateConnection.Create(_protocol, _connectionString, mRecvMessages);

		if (tmpConn == null)
			return false;

		tmpConn.setDaemon(true);
		tmpConn.start();
		mPlayers.add(tmpConn);

		Utils.LOG("Agora tamb�m ligado a: " + _connectionString);
		return true;
	}

	public void broadcastPlayerName()
	{
		if (Game.mIsPVPGame)
		{
			mMessageToSend.messageType = MessageType.PLAYER;
			mMessageToSend.eventType = EventType.NAME;
			mMessageToSend.valShort = RemoteConnections.mLocalID;
			mMessageToSend.setStringValue(Settings.PLAYER_NAME);
			Game.mRemoteConnections.broadcast(mMessageToSend);
		}
	}

	public void update()
	{

		// Verifica se ainda estamos ligados ao servidor.
		if (!mIsGameServer && !connectedToServer() && !Game.mGameIsOver)
		{
			Utils.LOG("Foi perdida a liga��o ao servidor... ");
			Game.mRemoteConnections = null;
			if (mGame != null)
				mGame.setGameState(new GameStateServerConnectionError(mGame, Strings.mStrings.get("lost_server")));
			else
				Game.goBackToActivities();
		}

		// TODO : comentar a linha abaixo na release.
		// Serve apenas para actualizar a estatistica de mensagens por segundo
		mRecvMessages.update();

		if (mAcceptingConnections)
			updateLocalServer();

		// Actualiza as liga��es j� existentes
		for (int i = 0; i < mPlayers.size(); i++)
			mPlayers.get(i).update();

		// Actualiza o servidor
		if (mGameServer != null)
			mGameServer.update();
	}

	private synchronized void updateLocalServer()
	{
		if (mLocalServer == null)
			return;

		// Verifica se existem novas liga��es disponiveis
		short nPlayers = (short) mPlayers.size();
		mLocalServer.getCachedConnections(mPlayers);

		if (mIsGameServer && nPlayers < mPlayers.size())
		{
			// Foram adicionados mais players por isso atribui-lhes um id
			mGameServer.giveIds(nPlayers);
		}

		mAcceptingConnections = !mLocalServer.mAllConnected;

		if (!mAcceptingConnections)
		{
			//
			// Entrar aqui significa se j� foram aceites o n�mero total de
			// liga��es previstas.

			// TODO : comentar este bloco na release
			Utils.LOG("Clientes ligados:");
			for (int i = 0; i < mPlayers.size(); i++)
				Utils.LOG(mPlayers.get(i).toString());

			if (mIsGameServer)
				mGameServer.mReadyToStart = true;
		}
	}

	/**
	 * Cria um server local que vai aceitar conex�es remotas.
	 * 
	 * @param _protocol
	 *            O protocolo a usar um dos {@link Protocols}.
	 * @param _connectionString
	 *            Dados da liga��o. Varia consoante o protocolo. TCP p.e:
	 *            "localhost:50087"
	 * @param _maxConnections
	 *            O n�mero m�ximo de conex�es a aceitar.
	 * @throws IOException
	 */
	public boolean acceptConnections(short _protocol, String _connectionString, short _maxConnections) throws IOException
	{
		int port = 50005;
		try
		{
			port = Integer.valueOf(_connectionString);
		}catch(NumberFormatException e)
		{
			return false;
		}
		
		
		switch (_protocol)
		{
		case Protocols.TCP:
			mLocalServer = new TCPLocalServer(mRecvMessages, port, _maxConnections);
			break;
		case Protocols.UDP:
			mLocalServer = new UDPLocalServer(mRecvMessages, port, _maxConnections);
			break;
		case Protocols.BLUETOOTH:
			mLocalServer = new BluetoothLocalServer(mRecvMessages);
			break;
		}

		if (null == mLocalServer)
			return false;

		mAcceptingConnections = true;
		mLocalServer.setDaemon(true);
		mLocalServer.start();

		return true;
	}

	public void broadcast(Message _msg)
	{
		Connection tmpConn;
		for (int i = 0; i < mPlayers.size(); i++)
		{
			tmpConn = mPlayers.get(i);

			if (!tmpConn.mIsConnected)
			{
				// Remove a conex�o da lista de conex�es activas
				mPlayers.remove(i);
				i--;
				continue;
			}

			tmpConn.sendMessage(_msg);
		}

		if (mGameServer != null)
			mGameServer.sendMessage(_msg);
	}

	public static RemoteConnections create(short _protocol, boolean _isServer, String _serverAddress)
	{
		RemoteConnections connections = new RemoteConnections(_isServer, _protocol);
		if (!_isServer)
		{

			// Cria um servidor local que vai aceitar liga��es por parte de
			// outros jogadores, num porto aleat�rio. Isto tem que ser lan�ado
			// antes da liga��o ao servidor porque � necess�rio indicar ao
			// servidor o porto em que vamos estar � escuta.
			Random r = new Random(System.nanoTime());
			Integer port = r.nextInt(100) + 50006;
			try
			{
				if (_protocol != Protocols.BLUETOOTH)
				{
					Utils.LOG("A ligar o servidor local no porto: " + port);
					connections.acceptConnections(_protocol, port.toString(), (short) 3);
					Utils.LOG("� espera de liga��es...");
				}
			} catch (IOException e)
			{
				Utils.LOG("Erro ao lan�ar o servidor local.");
				e.printStackTrace();
				return null;
			}

			// Liga ao servidor
			try
			{
				Utils.LOG("A tentar ligar ao servidor de jogo...");
				connections.connectToGameServer(_protocol, _serverAddress);
				Utils.LOG("Connectado ao servidor!");
			} catch (IOException e)
			{
				Utils.LOG("Erro ao ligar ao servidor de jogo.");
				e.printStackTrace();
				GameStateLoadingPVP.mFailedToConnectToServer = true;
				return null;
			}
		} else
		{

			// Inicializa o servidor
			try
			{
				String[] addressComponents = _serverAddress.split(":");

				Utils.LOG("A inicializar o servidor de jogo no porto " + addressComponents[1]);
				connections.acceptConnections(_protocol, addressComponents[1], (short) (Game.mNumberPlayers - 1));
				//if (_isServer)
					Utils.LOG("Server online!");
			} catch (IOException e)
			{
				Utils.LOG("Erro ao inicializar o servidor.");
				e.printStackTrace();
				return null;
			}
		}

		return connections;
	}

	public void closeAll(String _reason)
	{

		// Fecha tudo
		for (int i = 0; i < mPlayers.size(); i++)
		{
			try
			{
				mPlayers.get(i).disconnect(_reason);
				mPlayers.get(i).join();
			} catch (Throwable t)
			{
			}
		}

		if (mGameServer != null)
		{
			try
			{
				mGameServer.disconnect(_reason);
				mGameServer.join();
			} catch (Throwable t)
			{
			}
		}

		if (mLocalServer != null)
		{
			try
			{
				mLocalServer.stopReceiving();
				mLocalServer.join();
			} catch (Throwable t)
			{
			}
		}
	}
}