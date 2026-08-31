package uk.ac.rothamsted.neo4j.utils;

import java.time.Duration;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.SimpleQueryRunner;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransactionContext;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.UncheckedTimeoutException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.ac.ebi.utils.collections.PaginationIterator;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.opt.runcontrol.ReactorUtils;

/**
 * Utilities to work with the Project Reactor integration into Neo4j.
 *
 * TODO: write tests and examples.
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2024</dd></dl>
 *
 */
public class Neo4jUtils
{
	/**
	 * Default page size for paginated reads. We have set this value based on past experience.
	 */
	public static final long DEFAULT_PAGE_SIZE = 2500L;
	
	private static Logger log = LoggerFactory.getLogger ( Neo4jUtils.class );
	
	private Neo4jUtils () {}

		
	/**
	 * TODO: remove. This uses #reactiveRead(Function, Driver), which is silly, since the reactive
	 * result is switched back to a blocking context.
	 * 
	 * Use {@link #paginatedRead(BiFunction, Driver, Long)}
	 *  
	 * and is a bit of a hack, since it uses the offset as a parameter
	 * 
	 * An helper to deal with the pagination of read-only queries.
	 * 
	 * This is an {@link Iterator} based on a Cypher query that has OFFSET/LIMIT clauses.
	 * This is based on {@link PaginationIterator}.
	 * 
	 * @param callBack The Cypher query from which to get a page result. This is run 
	 * through {@link #reactiveRead(Function, Driver)}. This receives the current result offset 
	 * as second parameter, so that the query can return the publisher for the next page or null. Note
	 * that although the call back doesn't receive the page size, it typically will use the same 
	 * value that is passed to this method, as a value that doesn't vary across page queries.
	 * 
	 * @param neoDriver
	 * 
	 * @param pageSize The iterator buffers results in memory, with a buffer having the same size as pageSize, 
	 * so take this into account.
	 */
	private static Iterator<Record> _paginatedRead ( BiFunction<ReactiveTransactionContext, Long, Publisher<ReactiveResult>> callBack, Driver neoDriver, Long pageSize )
	{
		if ( pageSize == null ) pageSize = DEFAULT_PAGE_SIZE; // From past experience

		// As explained in PaginationIterator, here we can return the page elements iterator,
		// or null when the current page has not elements anymore.
		Function<Long, Iterator<Record>> pageSelector = offset ->
		{
		  Iterator<Record> pageItr = reactiveRead ( tx -> callBack.apply (tx, offset), neoDriver )
		    .toIterable ()
		    .iterator ();
		  
		  return pageItr.hasNext () ? pageItr : null;
		};
				
		return PaginationIterator.offsetBasedElementsIterator ( 
			pageSelector, pageSize
		);		
	}

	/**
	 * TODO: remove, as per {@link #_paginatedRead(BiFunction, Driver, Long)}
	 * 
	 * Default page size.
	 */
	private static Iterator<Record> _paginatedRead ( BiFunction<ReactiveTransactionContext, Long, Publisher<ReactiveResult>> callBack, Driver neoDriver )
	{
		return _paginatedRead ( callBack, neoDriver, null );
	}
	
	/**
	 * An helper to deal with the pagination of read-only queries.
	 * 
	 * This is an {@link Iterator} based on a Cypher query that has OFFSET/LIMIT clauses.
	 * This is based on {@link PaginationIterator}.
	 * 
	 * @param callBack The Cypher query from which to get a page result. This receives the current result offset 
	 * as second parameter, so that the query can return the publisher for the next page or null. Note
	 * that although the callback doesn't receive the page size, it typically will use the same 
	 * value that is passed to this method, as a value that doesn't vary across page queries. The callback 
	 * is run through one call to {@link Session#executeRead(TransactionCallback)} per page, and all the pages
	 * run under the same session.
	 * 
	 * @param neoDriver The Neo4j driver to use for the session. Note that if this is a {@link XNeo4jDriver}, 
	 * the session will be created with the database name set in it (ie, using {@link XNeo4jDriver#sessionConfigBuilder()}).
	 * 
	 * @param pageSize The iterator buffers results in memory, with a buffer having the same size as pageSize, 
	 * so take this into account.
	 * 
	 * @param transactionConfig The transaction config to use for each page query. 
	 * If null, the default config is used. This param is added here mainly to allow for
	 * a query timeout, see {@link #configureTransactionTimeout(Duration, org.neo4j.driver.TransactionConfig.Builder)}.
	 * 
	 */	
	public static <T> Iterator<T> paginatedRead ( 
		BiFunction<SimpleQueryRunner, Long, Iterator<T>> callBack, Driver neoDriver, 
		Long pageSize, TransactionConfig transactionConfig 
	)
	{
		if ( pageSize == null ) pageSize = DEFAULT_PAGE_SIZE; // From past experience
		
		// As explained in PaginationIterator, here we can return the page elements iterator,
		// or null when the current page has not elements anymore.
		
		// We need to keep the session open until the last page, so the page selector has 
		// to manage its life cycle. 
		Mutable<Session> session = new MutableObject<> ();
		Mutable<Transaction> transaction = new MutableObject<> ();

		SessionConfig.Builder scBuilder = neoDriver instanceof XNeo4jDriver 
			? ((XNeo4jDriver) neoDriver).sessionConfigBuilder () 
			: SessionConfig.builder ();
		SessionConfig sessionConfig = scBuilder
			.withDefaultAccessMode ( AccessMode.READ )
			.build ();
		Function<Long, Iterator<T>> pageSelector = offset ->
		{
			Session theSession = session.getValue ();
			if ( theSession == null ) 
				session.setValue ( theSession = neoDriver.session ( sessionConfig ) );
			else
				// At least one page already done, close the transaction about it
				transaction.getValue ().close ();
				
			// New page, new transaction
			Transaction theTx = transactionConfig == null 
				? theSession.beginTransaction ()
				: theSession.beginTransaction ( transactionConfig );
			transaction.setValue ( theTx );
						
			Iterator<T> pageItr = callBack.apply ( theTx, offset );
			
			if ( pageItr.hasNext () ) return pageItr;
			
			// No more pages, close the transaction and the session			
			theTx.close ();
			theSession.close ();
			return null;
		};
			
		return PaginationIterator.offsetBasedElementsIterator ( 
			pageSelector, pageSize
		);
	}
	
	/**
	 * Default page size.
	 *
	 */
	public static <T> Iterator<T> paginatedRead ( 
		BiFunction<SimpleQueryRunner, Long, Iterator<T>> callBack, Driver neoDriver, 
		TransactionConfig transactionConfig 
	)
	{
		return paginatedRead ( callBack, neoDriver, null, transactionConfig );
	}

	/**
	 * Default transaction config.
	 */
	public static <T> Iterator<T> paginatedRead ( 
		BiFunction<SimpleQueryRunner, Long, Iterator<T>> callBack, Driver neoDriver, 
		Long pageSize
	)
	{
		return paginatedRead ( callBack, neoDriver, pageSize, null );
	}
	
	/**
	 * Default page size and transaction config.
	 *
	 */
	public static <T> Iterator<T> paginatedRead ( 
		BiFunction<SimpleQueryRunner, Long, Iterator<T>> callBack, Driver neoDriver 
	)
	{
		return paginatedRead ( callBack, neoDriver, null, null );
	}
	
	
	/**
	 * Helper to process a Neo4j read query in a reactive style.
	 * 
	 * In a nutshell, prepares a {@link Flux} that publishes each record
	 * returned by the query in the callback.
	 * 
	 * This is a template based on the approach described
	 * 
	 * <a href = "https://neo4j.com/docs/java-manual/current/reactive">here</a> and
	 * <a href = "https://medium.com/neo4j/importing-data-into-neo4j-using-rxjs-ed017004bb25">here</>.
	 * 
	 * @param callBack this is passed to 
	 * {@link ReactiveSession#executeRead(ReactiveTransactionCallback)} and it's where you should run your 
	 * Cypher query and produce a {@link ReactiveResult}, from which we do downstream processing. Typically, 
	 * this is done via {@link ReactiveTransactionContext#run(String)} and its variants.
	 * 
	 * @param neoDriver obviously, you need a Neo4j driver to talk to.
	 * 
	 * @return a reactive {@link Flux} of {@link Record}.
	 * 
	 * TODO: add support for {@link TransactionConfig}.
	 * 
	 * TODO: we could return T, see {@link #reactivePaginatedRead(BiFunction, Driver, Long)} vs
	 * {@link #reactivePaginatedRead2Records(BiFunction, Driver, Long)}.
	 */
	public static Flux<Record> reactiveRead ( 
		Function<ReactiveTransactionContext, Publisher<ReactiveResult>> callBack,
		Driver neoDriver )
	{
		return Flux.usingWhen (
			// The reactive session is generated when a subscriber comes...
			Mono.fromSupplier ( () -> neoDriver.session ( ReactiveSession.class ) ),
			
			// ...and it's used in the closure, to spawn a Flux of records
			rsession -> rsession.executeRead ( tx ->
			  // This yields a ReactiveResult
				Mono.fromDirect ( callBack.apply ( tx ) )
				// which is mapped onto its records
				.flatMapMany ( ReactiveResult::records )
			), // executeRead(), usingWhen(), closure publisher
			
			ReactiveSession::close, // usingWhen(), flux cleanup in case of completion
			
			// usingWhen(), flux cleanup in case of error
			(rsession, ex) -> {
				rsession.close ();
				throw ExceptionUtils.buildEx ( 
					ClientException.class, ex, "Error while running reactive Neo4j query: $cause"
				);
			},
				
			// usingWhen(), flux cleanup in case of cancellation
			rsession -> {
				log.debug ( "Neo4j reactive query cancelled" );
				rsession.close ();
				return Flux.empty ();
			}
			
		); // usingWhen ()
	
	} // static reactiveRead()	
	
	
	/**
	 * Performs a reactive paginated read.
	 * 
	 * This is the reactive version of {@link #paginatedRead(BiFunction, Driver, Long)}.
	 * 
	 * Returns a {@link Flux} that is the concatenation of the (reactive) pages returned by the callback,
	 * The result is completely reactive, a new page publisher is requested only when the previous page has been consumed.
	 * 
	 * @param callBack The Cypher query from which to get a page result. This passes the current page
	 * to the query and typically the callback sees the same pageSize parameter that is passed here.
	 * This is passed to {@link ReactiveSession#executeRead(ReactiveTransactionCallback)} and the result
	 * it returns is mapped onto {@link ReactiveResult#records()}.
	 * 
	 * @param neoDriver the Neo4j driver to be used for the operation.
	 * 
	 * @param pageSize The page size, which we use to increment the offset for the next page, to be 
	 * handed to the callBack. Note that the callback typically sees and uses the same value to set the 
	 * LIMIT clause in the query.
	 * 
	 * TODO: do we need a transaction config here, as in {@link #paginatedRead(BiFunction, Driver, Long, TransactionConfig)}?
	 * The timeouts can be set via methods like {@link Flux#blockLast(Duration)}.
	 * 
	 */
	public static <T> Flux<T> reactivePaginatedRead (
		BiFunction<ReactiveTransactionContext, Long, ? extends Publisher<T>> callBack,
		Driver neoDriver, 
		Long pageSize 			
	)
	{	
		// All of this could be done with a couple of Flux.usingWhen(), I guess it's a matter of preference.
		//
		
		long pageSizeRO = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
				
		Flux<T> result = Flux.defer ( () ->
		{
			// A new state is created upon each subscription. This outer flux ensures one state
			// per subscription (though multiple subscriptions are rare).
			//
			class State {
	      long offset = 0;
	      boolean isPageEmpty = true;
	      ReactiveSession rsession = null;
			}
			
			State state = new State ();

			// One session for all the pages. We create it here, since the flux below is subscribed straight
			// after this outer subscription.
			state.rsession = neoDriver.session ( ReactiveSession.class );
			
			// The pages are obtained by combining the page flux and repeat() conditioned on 
			// page.isEmpty. 
			// The game is started upon subscription only 
			//
			Flux<T> allPagesFlux = Flux.defer ( () -> 
			{
				// We use the Neo straight instead of reactiveRead(), because the latter would use one session
				// per page, which is slightly less efficient
				Flux<T> pageFlux = Flux.from ( state.rsession.executeRead ( 
					tx -> callBack.apply ( tx, state.offset ) 
				))
			  .switchOnFirst ( (signal, flux) -> {
			  	// mark the state when the first element arrives
					state.isPageEmpty = !signal.hasValue ();
					// And continue with the flux
					return flux;
				})
				.doOnComplete ( () -> {
					// Page is over, move to the next one if available
					if ( !state.isPageEmpty ) state.offset += pageSizeRO;
				})
				.doOnError ( ex -> 
				{
					throw ExceptionUtils.buildEx ( 
						ClientException.class, ex, "Error while running reactive Neo4j query: $cause"
					);
				})
				.doOnCancel ( () -> {
					log.debug ( "Neo4j paginated reactive query cancelled at offset", state.offset );
				});
				
				return pageFlux;
			}) // defer() for allPagesFlux
			// After the current page, keep subscribing to the page flux again, which has advanced the offset
			// Do it until the last page
			.repeat ( () -> !state.isPageEmpty )
			.doFinally ( signal -> {
				// Whatever the end, clean up
				state.rsession.close ();
			});
			
			return allPagesFlux;
		}); // defer() for result
		
		return result;
	} // reactivePaginatedRead()
	
	
	/**
	 * Default page size.
	 */
	public static <T> Flux<T> reactivePaginatedRead (
		BiFunction<ReactiveTransactionContext, Long, ? extends Publisher<T>> callBack,
		Driver neoDriver 
	)
	{
		return reactivePaginatedRead ( callBack, neoDriver, null );
	}
	
	/**
	 * This is like {@link #reactivePaginatedRead(BiFunction, Driver, Long)}, except the callback
	 * is allowed to return a {@link Publisher} of {@link ReactiveResult}, that is directly what
	 * {@link ReactiveSession#executeRead(ReactiveTransactionCallback)} returns. We then map the
	 * reactive result onto its records.
	 *  
	 */
	public static Flux<Record> reactivePaginatedRead2Records (
		BiFunction<ReactiveTransactionContext, Long, ? extends Publisher<ReactiveResult>> callBack,
		Driver neoDriver, 
		Long pageSize 			
	)
	{
		return reactivePaginatedRead ( 
			(tx, offset) -> Mono.fromDirect ( callBack.apply ( tx, offset ) )
			.flatMapMany ( ReactiveResult::records ),
			neoDriver, pageSize
		);
	}

	/**
	 * Default page size.
	 */
	public static Flux<Record> reactivePaginatedRead2Records (
		BiFunction<ReactiveTransactionContext, Long, ? extends Publisher<ReactiveResult>> callBack,
		Driver neoDriver 
	)
	{
		return reactivePaginatedRead2Records ( callBack, neoDriver, null );
	}
	
	
	
	/**
	 * Builds a {@link TransactionConfig} with the given timeout, if any.
	 * 
	 * If {@code txCfgBuilder} is null, builds a new config set with the timeout.
	 * If timeout is null, it ignores it.
	 * If both are null, returns a default config.
	 * 
	 * Note that we don't deal with any caching, if you want to reuse tx configs, 
	 * you have to take care of it on your own.
	 * 
	 * TODO: write tests.
	 */
	public static TransactionConfig configureTransactionTimeout ( Duration timeout, TransactionConfig.Builder txCfgBuilder )
	{
		if ( txCfgBuilder == null ) txCfgBuilder = TransactionConfig.builder ();
		if ( timeout != null ) txCfgBuilder.withTimeout ( timeout );
		
		return txCfgBuilder.build ();
	}
	
	/**
	 * Defaults to a null/default transaction config, to which the timeout is added, if any.
	 */
	public static TransactionConfig configureTransactionTimeout ( Duration timeout )
	{
		return configureTransactionTimeout ( timeout, null );
	}
}
