package com.bomber.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/**
 * D� muito jeito poder iterar uma colec��o por isso existir� apenas um iterador
 * 
 * Implementa Iterable<T>
 */
public class ObjectsPool<T extends PoolObject> implements Iterable<T> {
	private Stack<T> mFreeObjects = new Stack<T>();
	private ArrayList<T> mUsedObjects = new ArrayList<T>();
	private Stack<Short> mFreePositions = new Stack<Short>();
	private Factory<T> mFactory;
	private ObjectsPoolIterator<T> mObjectsIterator;

	private int mUUID;
	public short mLenght = 0;
	
	public ObjectsPool(boolean _useUUID, short _initialQuantity, Factory<T> _factory) {

		mFactory = _factory;

		if(_useUUID)
			mUUID = Utils.getNextUUID();
		
		// Inicializa os containers
		if (null != mFactory)
			allocateNewObjects(_initialQuantity);

		mObjectsIterator = new ObjectsPoolIterator<T>(mUsedObjects);
	}
	public ObjectsPool(short _initialQuantity, Factory<T> _factory) {

		mFactory = _factory;

		mUUID = Utils.getNextUUID();
		
		// Inicializa os containers
		if (null != mFactory)
			allocateNewObjects(_initialQuantity);

		mObjectsIterator = new ObjectsPoolIterator<T>(mUsedObjects);
	}

	public void clear()
	{
		clear(false);
	}

	public void clear(boolean _reorderFreePositions)
	{
		for (T tmp : this)
			releaseObject(tmp);

		if(mFactory==null)
		{
			mFreePositions.clear();
			mUsedObjects.clear();
		}
		
		if (_reorderFreePositions)
		{
			mFreePositions.clear();
			for (Short i = (short) (mFreeObjects.size()-1); i >=0 ; i--)
				mFreePositions.push(i);
		}
	}

	private void allocateNewObjects(short _quantity)
	{
		if (null == mFactory)
			throw new UnsupportedOperationException();

		short freePositionStart = (short) mUsedObjects.size();
		for (short i = 0; i < _quantity; freePositionStart++, i++)
		{
			T tmpObject = mFactory.create();
			
			mFreeObjects.push(tmpObject);
			mFreePositions.push(freePositionStart);
			mUsedObjects.add(null);
		}
	}

	public void addObject(T _obj)
	{
		// Verifica se existem lugares livres no array
		if (!mFreePositions.empty())
		{
			_obj.setIndex(mUUID, mFreePositions.pop());
			mUsedObjects.set(_obj.getIndex(), _obj);
		} else
		{
			_obj.setIndex(mUUID, (short) mUsedObjects.size());
			mUsedObjects.add(_obj);
		}
		
		mLenght++;
	}

	/**
	 * coloca o objecto devolvido(obtido do topo da stack mFreeObjects) na
	 * posi��o do topo da mFreePositions.
	 */
	public T getFreeObject()
	{
		if (null == mFactory)
			throw new UnsupportedOperationException();

		// Verifica se existem objectos livres disponiveis
		if (mFreeObjects.size() == 0)
			allocateNewObjects((short) 1);

		// Obtem a posi��o onde vai ser inserido no array de objectos ocupados
		Short insertPos = mFreePositions.pop();

		// Obtem o objecto a devolver
		T result = mFreeObjects.pop();
		result.reset();

		// Actualiza o index do objecto para o libertar mais tarde
		result.setIndex(mUUID, insertPos);

		mUsedObjects.set(insertPos, result);

		mLenght++;
		
		return result;
	}

	public void releaseObject(T obj)
	{
		short freeIndex = obj.getIndex();

		// Se n�o tiver sido providenciado uma factory ent�o significa que os
		// objectos
		// foram inseridos � m�o e isto significa que n�o s�o reaproveit�veis,
		// logo
		// n�o vale a pena guardar
		if (null != mFactory)
			mFreeObjects.push(obj);

		mFreePositions.push(freeIndex);
		mUsedObjects.set(freeIndex, null);
		
		mLenght--;
	}

	@Override
	public Iterator<T> iterator()
	{
		// Faz reset ao iterador e devolve sempre o mesmo
		mObjectsIterator.reset();
		return mObjectsIterator;
	}
}