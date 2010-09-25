package org.cocos2d.events;

import android.view.MotionEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import org.cocos2d.protocols.CCTouchDelegateProtocol;

/** CCTouchDispatcher.
  Singleton that handles all the touch events.
  The dispatcher dispatches events to the registered TouchHandlers.
  There are 2 different type of touch handlers:
  - Standard Touch Handlers
  - Targeted Touch Handlers

  The Standard Touch Handlers work like the CocoaTouch touch handler: a set of touches is passed to the delegate.
  On the other hand, the Targeted Touch Handlers only receive 1 touch at the time, and they can "swallow" touches (avoid the propagation of the event).

  Firstly, the dispatcher sends the received touches to the targeted touches.
  These touches can be swallowed by the Targeted Touch Handlers. If there are still remaining touches, then the remaining touches will be sent
  to the Standard Touch Handlers.

  @since v0.8.0
*/
/*
 * TODO: FIXME: CCStandardTouchDelegateProtocol, CCTargetedTouchDelegateProtocol is not fully ported
 */
public class CCTouchDispatcher {
    public enum ccTouchSelectorFlag {
    	ccTouchSelectorNoneBit (1 << 0),
        ccTouchSelectorBeganBit (1 << 0),
        ccTouchSelectorMovedBit (1 << 1),
        ccTouchSelectorEndedBit (1 << 2),
        ccTouchSelectorCancelledBit (1 << 3),
        ccTouchSelectorAllBits ( ccTouchSelectorBeganBit.flag | ccTouchSelectorMovedBit.flag 
                                    | ccTouchSelectorEndedBit.flag | ccTouchSelectorCancelledBit.flag);
    	ccTouchSelectorFlag(int val) {
    		this.flag = val;
    	}
    	
    	public int getFlag() {
    		return flag;
    	}
    	
    	private final int flag;
    }

    class ccTouchHandlerHelperData {
        public String touchesSel;
        public String touchSel;
        public int ccTouchSelectorType;
    }

    public static final int ccTouchBegan        = 0;
    public static final int ccTouchMoved        = 1;
    public static final int ccTouchEnded        = 2;
    public static final int ccTouchCancelled    = 3;
    public static final int ccTouchMax          = 4;

    public static final boolean kEventHandled = true;
    public static final boolean kEventIgnored = false;
 /*
@interface CCTouchDispatcher : NSObject <EAGLTouchDelegate>
{
	NSMutableArray	*targetedHandlers;
	NSMutableArray	*standardHandlers;

	BOOL			locked;
	BOOL			toAdd;
	BOOL			toRemove;
	NSMutableArray	*handlersToAdd;
	NSMutableArray	*handlersToRemove;
	BOOL			toQuit;

	// 4, 1 for each type of event
	struct ccTouchHandlerHelperData handlerHelperData[ccTouchMax];
}*/
   
    private CopyOnWriteArrayList<CCTouchHandler> touchHandlers;
    /** Whether or not the events are going to be dispatched. Default: YES */
    private boolean dispatchEvents;

    public boolean getDispatchEvents() {
        return dispatchEvents;
    }

    public void setDispatchEvents(boolean b) {
        dispatchEvents = b;
    }

    private static CCTouchDispatcher _sharedDispatcher;

    /** singleton of the CCTouchDispatcher */
    public static CCTouchDispatcher sharedDispatcher() {
        if (_sharedDispatcher == null) {
            synchronized (CCTouchDispatcher.class) {
                if (_sharedDispatcher == null) {
                    _sharedDispatcher = new CCTouchDispatcher();
                }
            }
        }
        return _sharedDispatcher;
    }

    protected CCTouchDispatcher() {
        dispatchEvents = true;
        touchHandlers = new CopyOnWriteArrayList<CCTouchHandler>();
    }

    //
    // handlers management
    //

    /** Adds a standard touch delegate to the dispatcher's list.
     See StandardTouchDelegate description.
     IMPORTANT: The delegate will be retained.
     */
    // -(void) addStandardDelegate:(id<CCStandardTouchDelegate>) delegate priority:(int)priority;


    private void addHandler(CCTouchHandler handler) {
        int i = 0;
        for( CCTouchHandler h : touchHandlers ) {
            if( h.getPriority() < handler.getPriority() )
                i++;

            if( h.getDelegate() == handler.getDelegate() )
                throw new RuntimeException("Delegate already added to touch dispatcher.");
        }
        touchHandlers.add(i, handler);
    }

    public void addDelegate(CCTouchDelegateProtocol delegate, int prio) {
        addHandler(new CCTouchHandler(delegate, prio));
    }
    
    public void removeDelegate(CCTouchDelegateProtocol delegate) {
        if( delegate == null )
            return;

        for( CCTouchHandler handler : touchHandlers ) {
            if( handler.getDelegate() == delegate ) {
                touchHandlers.remove(handler);
                break;
            }
        }
    }

    public void removeAllDelegates() {
        touchHandlers.clear();
    }

    /** Changes the priority of a previously added delegate. The lower the number,
      the higher the priority */
    // -(void) setPriority:(int) priority forDelegate:(id) delegate;
    public void setPriority(int priority, CCTouchHandler delegate) {
        if( delegate == null )
            throw new RuntimeException("Got null touch delegate");

        int i = 0;
        for( CCTouchHandler handler : touchHandlers ) {
            if( handler.getDelegate() == delegate ) break;
            i++;
        }

        if( i == touchHandlers.size() )
            throw new RuntimeException("Touch delegate not found");

        CCTouchHandler handler = touchHandlers.get(i);

        if( handler.getPriority() != priority ) {
            handler.setPriority(priority);

            touchHandlers.remove(handler);
            addHandler(handler);
        }
    }

    //
    // dispatch events
    //
    public void touchesBegan(MotionEvent event) {
        if( dispatchEvents )  {

            for( CCTouchHandler handler : touchHandlers ) {
                if( handler.ccTouchesBegan(event) == kEventHandled )
                    break;
            }
        }
    }

    public void touchesMoved(MotionEvent event) {
        if( dispatchEvents )  {

            for( CCTouchHandler handler : touchHandlers ) {
                if( handler.ccTouchesMoved(event) == kEventHandled )
                    break;
            }
        }
    }

    public void touchesEnded(MotionEvent event) {
        if( dispatchEvents )  {

            for( CCTouchHandler handler : touchHandlers ) {
                if( handler.ccTouchesEnded(event) == kEventHandled )
                    break;
            }
        }
    }

    public void touchesCancelled(MotionEvent event) {
        if( dispatchEvents )  {

            for( CCTouchHandler handler : touchHandlers ) {
                if( handler.ccTouchesCancelled(event) == kEventHandled )
                    break;
            }
        }
    }
}

/** Adds a targeted touch delegate to the dispatcher's list.
 See TargetedTouchDelegate description.
 IMPORTANT: The delegate will be retained.
 */
// -(void) addTargetedDelegate:(id<CCTargetedTouchDelegate>) delegate priority:(int)priority swallowsTouches:(BOOL)swallowsTouches;

/** Removes a touch delegate.
 The delegate will be released
 */
// -(void) removeDelegate:(id) delegate;

/** Removes all touch delegates, releasing all the delegates */
// -(void) removeAllDelegates;
//
//
/*

static CCTouchDispatcher *sharedDispatcher = nil;

+(CCTouchDispatcher*) sharedDispatcher
{
	@synchronized(self) {
		if (sharedDispatcher == nil)
			sharedDispatcher = [[self alloc] init]; // assignment not done here
	}
	return sharedDispatcher;
}

+(id) allocWithZone:(NSZone *)zone
{
	@synchronized(self) {
		NSAssert(sharedDispatcher == nil, @"Attempted to allocate a second instance of a singleton.");
		return [super allocWithZone:zone];
	}
	return nil; // on subsequent allocation attempts return nil
}

-(id) init
{
	if((self = [super init])) {
	
		dispatchEvents = YES;
		targetedHandlers = [[NSMutableArray alloc] initWithCapacity:8];
		standardHandlers = [[NSMutableArray alloc] initWithCapacity:4];
		
		handlersToAdd = [[NSMutableArray alloc] initWithCapacity:8];
		handlersToRemove = [[NSMutableArray alloc] initWithCapacity:8];
		
		toRemove = NO;
		toAdd = NO;
		toQuit = NO;
		locked = NO;

		handlerHelperData[ccTouchBegan] = (struct ccTouchHandlerHelperData) {@selector(ccTouchesBegan:withEvent:),@selector(ccTouchBegan:withEvent:),ccTouchSelectorBeganBit};
		handlerHelperData[ccTouchMoved] = (struct ccTouchHandlerHelperData) {@selector(ccTouchesMoved:withEvent:),@selector(ccTouchMoved:withEvent:),ccTouchSelectorMovedBit};
		handlerHelperData[ccTouchEnded] = (struct ccTouchHandlerHelperData) {@selector(ccTouchesEnded:withEvent:),@selector(ccTouchEnded:withEvent:),ccTouchSelectorEndedBit};
		handlerHelperData[ccTouchCancelled] = (struct ccTouchHandlerHelperData) {@selector(ccTouchesCancelled:withEvent:),@selector(ccTouchCancelled:withEvent:),ccTouchSelectorCancelledBit};
		
	}
	
	return self;
}

-(void) dealloc
{
	[targetedHandlers release];
	[standardHandlers release];
	[handlersToAdd release];
	[handlersToRemove release];
	[super dealloc];
}

//
// handlers management
//

#pragma mark TouchDispatcher - Add Hanlder

-(void) forceAddHandler:(CCTouchHandler*)handler array:(NSMutableArray*)array
{
	NSUInteger i = 0;
	
	for( CCTouchHandler *h in array ) {
		if( h.priority < handler.priority )
			i++;
		
		if( h.delegate == handler.delegate )
			[NSException raise:NSInvalidArgumentException format:@"Delegate already added to touch dispatcher."];
	}
	[array insertObject:handler atIndex:i];		
}

-(void) addStandardDelegate:(id<CCStandardTouchDelegate>) delegate priority:(int)priority
{
	CCTouchHandler *handler = [CCStandardTouchHandler handlerWithDelegate:delegate priority:priority];
	if( ! locked ) {
		[self forceAddHandler:handler array:standardHandlers];
	} else {
		[handlersToAdd addObject:handler];
		toAdd = YES;
	}
}

-(void) addTargetedDelegate:(id<CCTargetedTouchDelegate>) delegate priority:(int)priority swallowsTouches:(BOOL)swallowsTouches
{
	CCTouchHandler *handler = [CCTargetedTouchHandler handlerWithDelegate:delegate priority:priority swallowsTouches:swallowsTouches];
	if( ! locked ) {
		[self forceAddHandler:handler array:targetedHandlers];
	} else {
		[handlersToAdd addObject:handler];
		toAdd = YES;
	}
}

#pragma mark TouchDispatcher - removeDelegate

-(void) forceRemoveDelegate:(id)delegate
{
	// XXX: remove it from both handlers ???
	
	for( CCTouchHandler *handler in targetedHandlers ) {
		if( handler.delegate == delegate ) {
			[targetedHandlers removeObject:handler];
			break;
		}
	}
	
	for( CCTouchHandler *handler in standardHandlers ) {
		if( handler.delegate == delegate ) {
			[standardHandlers removeObject:handler];
			break;
		}
	}	
}

-(void) removeDelegate:(id) delegate
{
	if( delegate == nil )
		return;
	
	if( ! locked ) {
		[self forceRemoveDelegate:delegate];
	} else {
		[handlersToRemove addObject:delegate];
		toRemove = YES;
	}
}

#pragma mark TouchDispatcher  - removeAllDelegates

-(void) forceRemoveAllDelegates
{
	[standardHandlers removeAllObjects];
	[targetedHandlers removeAllObjects];
}
-(void) removeAllDelegates
{
	if( ! locked )
		[self forceRemoveAllDelegates];
	else
		toQuit = YES;
}

#pragma mark Changing priority of added handlers

-(void) setPriority:(int) priority forDelegate:(id) delegate
{
	NSAssert(NO, @"Set priority no implemented yet. Don't forget to report this bug!");
}


//
// dispatch events
//
-(void) touches:(NSSet*)touches withEvent:(UIEvent*)event withTouchType:(unsigned int)idx;
{
	NSAssert(idx >=0 && idx < 4, @"Invalid idx value");

	id mutableTouches;
	locked = YES;
	
	// optimization to prevent a mutable copy when it is not necessary
	unsigned int targetedHandlersCount = [targetedHandlers count];
	unsigned int standardHandlersCount = [standardHandlers count];	
	BOOL needsMutableSet = (targetedHandlersCount && standardHandlersCount);
	
	mutableTouches = (needsMutableSet ? [touches mutableCopy] : touches);

	struct ccTouchHandlerHelperData helper = handlerHelperData[idx];
	//
	// process the target handlers 1st
	//
	if( targetedHandlersCount > 0 ) {
		for( UITouch *touch in touches ) {
			for(CCTargetedTouchHandler *handler in targetedHandlers) {
				
				BOOL claimed = NO;
				if( idx == ccTouchBegan ) {
					claimed = [handler.delegate ccTouchBegan:touch withEvent:event];
					if( claimed )
						[handler.claimedTouches addObject:touch];
				} 
				
				// else (moved, ended, cancelled)
				else if( [handler.claimedTouches containsObject:touch] ) {
					claimed = YES;
					if( handler.enabledSelectors & helper.type )
						[handler.delegate performSelector:helper.touchSel withObject:touch withObject:event];
					
					if( helper.type & (ccTouchSelectorCancelledBit | ccTouchSelectorEndedBit) )
						[handler.claimedTouches removeObject:touch];
				}
					
				if( claimed && handler.swallowsTouches ) {
					if( needsMutableSet )
						[mutableTouches removeObject:touch];
					break;
				}
			}
		}
	}
	
	//
	// process standard handlers 2nd
	//
	if( standardHandlersCount > 0 && [mutableTouches count]>0 ) {
		for( CCTouchHandler *handler in standardHandlers ) {
			if( handler.enabledSelectors & helper.type )
				[handler.delegate performSelector:helper.touchesSel withObject:mutableTouches withObject:event];
		}
	}
	if( needsMutableSet )
		[mutableTouches release];
	
	//
	// Optimization. To prevent a [handlers copy] which is expensive
	// the add/removes/quit is done after the iterations
	//
	locked = NO;
	if( toRemove ) {
		toRemove = NO;
		for( id delegate in handlersToRemove )
			[self forceRemoveDelegate:delegate];
		[handlersToRemove removeAllObjects];
	}
	if( toAdd ) {
		toAdd = NO;
		for( CCTouchHandler *handler in handlersToAdd ) {
			Class targetedClass = [CCTargetedTouchHandler class];
			if( [handler isKindOfClass:targetedClass] )
				[self forceAddHandler:handler array:targetedHandlers];
			else
				[self forceAddHandler:handler array:standardHandlers];
		}
		[handlersToAdd removeAllObjects];
	}
	if( toQuit ) {
		toQuit = NO;
		[self forceRemoveAllDelegates];
	}
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
	if( dispatchEvents )
		[self touches:touches withEvent:event withTouchType:ccTouchBegan];
}
- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
	if( dispatchEvents ) 
		[self touches:touches withEvent:event withTouchType:ccTouchMoved];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
	if( dispatchEvents )
		[self touches:touches withEvent:event withTouchType:ccTouchEnded];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
	if( dispatchEvents )
		[self touches:touches withEvent:event withTouchType:ccTouchCancelled];
}
*/
