/*
 *
 * Copyright (c) Immotronic, 2012
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of ubikit-core, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package org.ubikit.event.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ubikit.AbstractPhysicalEnvironmentModelEvent;
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventGate;
import org.ubikit.event.EventListener;



public class EventGateImpl implements EventGate
{
	private static final int INCOMING_MESSAGE_QUEUE_SIZE = 20;
	private final List<EventGateImpl> linkPartners;
	private final BlockingQueue<AbstractEvent> incomingEvents;
	private final List<EventListener> listeners;
	private final List<EventListener> listenerAddingTemporaryList;
	private final List<EventListener> listenerRemovingTemporaryList;
	private final ReentrantLock listenersLock;
	private final Runnable eventDelivererTask;
	private final String name; // USAGE : for debugging purpose only.

	private volatile boolean running;

	final Logger logger = LoggerFactory.getLogger(EventGateImpl.class);

	private class EventDeliverer implements Runnable
	{

		@Override
		public void run()
		{
			running = true;
			while (!Thread.currentThread().isInterrupted())
			{
				try
				{
					if (logger.isDebugEnabled())
					{
						logger.debug("{}: ....Waiting for events....", name);
					}

					AbstractEvent event = incomingEvents.take();

					if (logger.isDebugEnabled())
					{
						logger.debug(
							"{}: ---- Delivering {} IS ON GOING... [{} listeners]",
							name,
							event.getClass().getName(),
							listeners.size());
						
						logger.debug(
							"{}: ----   * event date: {}",
							name,
							((AbstractPhysicalEnvironmentModelEvent) event).getDate());
					}

					listenersLock.lock(); // ### LOCKING access to listeners list.
					try
					{
						if (logger.isDebugEnabled())
						{
							logger.debug("{}: Lock acquired ", name);
						}
						int lcount = 0;
						for (EventListener eventListener : listeners)
						{
							if (logger.isDebugEnabled())
							{
								lcount++;
								
								logger.debug(
									"{}: * Notifying listener {}: {}",
									name,
									lcount,
									eventListener.getClass().getName());
							}

							try
							{
								if (logger.isDebugEnabled())
								{
									logger.debug("{}: --> {} delivering to {}", name, event
										.getClass()
										.getName(), eventListener.getClass().getName());
								}
								event.deliverTo(eventListener);
							}
							catch (ClassCastException exception)
							{}
							catch (Exception e)
							{
								logger.error(
									"{}: ### Exception occurs when {} received {}",
									name,
									eventListener.getClass().getName(),
									event.getClass().getName(),
									e);
							}
						}

						if (logger.isDebugEnabled())
						{
							logger.debug("{}: ---- Delivering {} IS DONE.", name, event
								.getClass()
								.getName());
						}

						if (!listenerAddingTemporaryList.isEmpty())
						{
							synchronized (listenerAddingTemporaryList)
							{
								for (EventListener eventListener : listenerAddingTemporaryList)
								{
									listeners.add(eventListener);
									if (logger.isDebugEnabled())
									{
										logger.debug(
											"{}: {} finally added.",
											((name.equals("")) ? this : name),
											eventListener.getClass().getName());
									}
								}

								listenerAddingTemporaryList.clear();
							}
						}

						if (!listenerRemovingTemporaryList.isEmpty())
						{
							synchronized (listenerRemovingTemporaryList)
							{
								for (EventListener eventListener : listenerRemovingTemporaryList)
								{
									listeners.remove(eventListener);
									if (logger.isDebugEnabled())
									{
										logger.debug(
											"{}: {} finally removed.",
											((name.equals("")) ? this : name),
											eventListener.getClass().getName());
									}
								}

								listenerRemovingTemporaryList.clear();
							}
						}
					}
					finally
					{
						listenersLock.unlock(); // ### UNLOCKING access to listeners list.
						logger.debug("{}: Lock released ", name);
					}
				}
				catch (InterruptedException e)
				{
					if (logger.isDebugEnabled())
					{
						logger.debug("{} was interrupted.", name);
					}
					break;
				}
			}

			running = false;
			if (logger.isDebugEnabled())
			{
				logger.debug("{} has terminated.", name);
			}
		}
	}






	private boolean removePartner(EventGateImpl partner)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("{} REMOVE LINK WITH {}", partner, ((name.equals("")) ? this : name));
		}
		return linkPartners.remove(partner);
	}






	protected String getName()
	{
		return this.name;
	}






	public EventGateImpl()
	{
		this("");
	}






	public EventGateImpl(String name)
	{
		this.name = (name == null) ? "" : name;
		running = false;
		linkPartners = Collections.synchronizedList(new ArrayList<EventGateImpl>());
		incomingEvents = new ArrayBlockingQueue<AbstractEvent>(INCOMING_MESSAGE_QUEUE_SIZE);
		listeners = new ArrayList<EventListener>();
		listenerAddingTemporaryList = new ArrayList<EventListener>();
		listenerRemovingTemporaryList = new ArrayList<EventListener>();
		listenersLock = new ReentrantLock();
		eventDelivererTask = new EventDeliverer();
	}






	public Runnable getEventDelivererTask()
	{
		return eventDelivererTask;
	}






	@Override
	public void linkTo(EventGate partner)
	{
		if (!linkPartners.contains(partner))
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("{} LINK TO {}", ((name.equals("")) ? this : name), ((partner
					.equals("")) ? partner : ((EventGateImpl) partner).name));
			}
			linkPartners.add((EventGateImpl) partner);
			partner.linkTo(this);
		}
	}






	@Override
	public void unlink(EventGate partner)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("{} UNLINK FROM {}", ((name.equals("")) ? this : name), ((partner
				.equals("")) ? partner : ((EventGateImpl) partner).name));
		}
		if (removePartner((EventGateImpl) partner))
		{
			((EventGateImpl) partner).removePartner(this);
		}
	}






	@Override
	public void unlinkAll()
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("{} UNLINK ALL ", ((name.equals("")) ? this : name));
		}
		synchronized (linkPartners)
		{
			Iterator<EventGateImpl> it = linkPartners.iterator();
			while (it.hasNext())
			{
				it.next().removePartner(this);
				it.remove();
			}
		}
	}






	@Override
	public void postEvent(AbstractEvent e)
	{
		synchronized (linkPartners)
		{
			for (EventGateImpl gate : linkPartners)
			{
				gate.onIncomingEvent(e);
			}
		}
	}






	@Override
	public void postLocalEvent(AbstractEvent e)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("{}: Dispatching local event {}", ((name.equals("")) ? this : name), e
				.getClass()
				.getName());
		}
		onIncomingEvent(e);
	}






	@Override
	public void postEvents(List<AbstractEvent> events)
	{
		synchronized (linkPartners)
		{
			for (EventGateImpl gate : linkPartners)
			{
				for (AbstractEvent e : events)
				{
					gate.onIncomingEvent(e);
				}
			}
		}
	}






	@Override
	public void addListener(EventListener listener)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug(
				"{}: Adding an event listener: {}",
				((name.equals("")) ? this : name),
				listener.getClass().getName());
		}
		listenersLock.lock();
		try
		{
			if (listenersLock.getHoldCount() < 2)
			{
				listeners.add(listener);
				if (logger.isDebugEnabled())
				{
					logger.debug("{}: {} is a new listener (now {} are listening)", ((name
						.equals("")) ? this : name), listener.getClass().getName(), listeners
						.size());
				}
			}
			else
			{
				synchronized (listenerAddingTemporaryList)
				{
					listenerAddingTemporaryList.add(listener);
					if (logger.isDebugEnabled())
					{
						logger.debug(
							"{}: {} is a new listener (adding delayed to avoid a deadlock)",
							((name.equals("")) ? this : name),
							listener.getClass().getName());
					}
				}
			}
		}
		finally
		{
			listenersLock.unlock();
		}
	}






	@Override
	public void removeListener(EventListener listener)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug(
				"{}: Removing an event listener: {}",
				((name.equals("")) ? this : name),
				listener.getClass().getName());
		}
		listenersLock.lock();
		try
		{
			if (listenersLock.getHoldCount() < 2)
			{
				listeners.remove(listener);
				if (logger.isDebugEnabled())
				{
					logger.debug("{}: {} is NO MORE a listener (now {} are listening)", ((name
						.equals("")) ? this : name), listener.getClass().getName(), listeners
						.size());
				}
			}
			else
			{
				synchronized (listenerRemovingTemporaryList)
				{
					listenerRemovingTemporaryList.add(listener);
					if (logger.isDebugEnabled())
					{
						logger.debug(
							"{}: {} is NO MORE a listener (removing delayed to avoid a deadlock)",
							((name.equals("")) ? this : name),
							listener.getClass().getName());
					}
				}
			}
		}
		finally
		{
			listenersLock.unlock();
		}
	}






	@Override
	public void clearAllListeners()
	{
		listenersLock.lock();
		try
		{
			listeners.clear();
		}
		finally
		{
			listenersLock.unlock();
		}
	}






	private void onIncomingEvent(AbstractEvent event)
	{
		if (running)
		{
			incomingEvents.offer(event);
		}
		else
		{
			if (logger.isDebugEnabled())
			{
				logger.debug(
					"{}: EventDeliverer is not runnging: Cannot deliver anything !",
					((name.equals("")) ? this : name));
			}
		}
	}
}
