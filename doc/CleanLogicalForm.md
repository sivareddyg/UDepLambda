##Notes on the generation of "clean logical form" from dep lambda expression

The information here is based on a 
current reading of the code and some correspondence with Siva.

Here is how post-processing works. (See [Logical Vocabulary Doc](https://github.com/sivareddyg/UDepLambda/blob/master/doc/LogicalVocabulary.md) for a description of the predicates.)

  * Process all literals, one at a time. 
       * `p_EVENT_w-<Index>-<Word>(V)` atom: add (V, Index-1) to `varToEvent`.
       * `p_TYPE_w-<Index>-<Word>(V)` atom: add (V, Index-1) to `varToEntity`.
       * `p_EQUAL(V,W)`: add (V,W) to `equalPairs`.
       * `p_CONJ(A, B, C)`: add (A,[B,C]) to `varToConj`.
  * Collect all literals starting with p_ in mainPredicates.
  * Invoke `cleanVarToEntity`: if a var is mapped to multiple entities (in `varToEntity`), prefer a named entity.
  * Invoke `cleanVarToEvents`: if a var is mapped to multiple events (in `varToEvent`), prefer NOT thenamed event. (Reason given below (*) by Siva.)
  * Invoke `populateConj`: For entry `(A,[B,C])` in `varToConj`, recursively follow `B`, `C` to find all possible sources of entities, and add them under `A` in `varToEntity`. Similarly for events.
  * Invoke `populateEquals`: perform transitive closure of equalities in `equalPairs` to form sets of  equivalent terms in `unifiedSets`.
  * Now perform the same transitive closure in `varToEntity`: each var `V` mapped to a list of entities  `X` is now mapped to the list of all entities that any variable equated to `W` is mapped to. Similarly for varToEvents.
  
This preliminary work done, enter the main loop `cleanedPredicates`, where each mainPredicate is examined one by one. Below define the "entity rep for `X`" as `X:x` if `X` does not name an entity, and
  `X:m.<WX>` if it does and the corresponding word is `<WX>`.
  
  * `p_EVENT.ENTITY_arg<i> (E, F)` or `p_EVENT.ENTITY_<DEP>.w-<I>-<Word> (E, F)` ==>
      * `W.<Word>(V:e, <XX>)`  if V names an entity in the utterance
      * `<Word>(V:e, <XX>)`   otherwise
  * (where `W` is the word at index `V`, for every `V` in `varToEvent` that `E` is mapped to, and `<XX>` is the entity rep for `X`, for every `X` in `varToEntity`  that `F` is mapped to.)
  * `p_EVENT.EVENT_arg1:b(E, F)`: similar to above. 
  * `p_COUNT:b (C, R)` ==>  `COUNT(C1:x, R1:x)`, for every entry `C1 `for `C` in `varToEntity` and `R1` for `R`.
  * `p_EVENT_w-<I>-<WORD>:u(V)` ==> `<WORD>(E:e)` for every entry `E` for `V` in `varToEvent`.
  * `p_TYPE_w-<I>-<WORD>:u(V)` ==> `<WORD>(I:s, <XX>)` where `<XX>` is the entity rep for `X`, for every `X` in `varToEntity` that `V` is mapped to.
  * `p_TYPEMOD_w-<I>-<WORD>:u(V)` ==> `<WORD>(I:s, <XX>)` where `<XX>` is the entity rep for `X`, for every `X` in `varToEntity` that `V` is mapped to.
  * `p_EVENTMOD_w-<I>-<WORD>:u(V)` ==> `<WORD>(I:s, X:e)` for every `X` in `varToEvents` that `V` is mapped to.
  * `p_TARGET:u (V) ==> QUESTION(<XX>)` where `<XX>` is the entity rep for `X`, for every `X` in `varToEntity` that `V` is mapped to.
    
###Note from Siva on preference for not named events
      
      Now cleanVarToEvents: if a var is mapped to multiple events, prefer NOT the named event? (Why?)

Take appositive constructions:
```Trump, the president of US, visited UK.```

Here current output is 

 ```
 (lambda $0:<a,e>
  (exists:ex $1:<a,e>
    (and:c
      (exists:ex $2:<a,e>
        (and:c
          (p_EVENT_w-8-visited:u $0)
          (p_TYPE_w-9-uk:u $2)
          (p_EVENT.ENTITY_arg2:b $0 $2)))
      (and:c
        (and:c
          (p_TYPE_w-1-trump:u $1)
          (p_EVENT_w-1-trump:u $1)
          (p_EVENT.ENTITY_arg0:b $1 $1))
        (exists:ex $3:<a,e>
          (and:c
            (and:c
              (and:c
                (p_TYPE_w-4-president:u $1)
                (p_EVENT_w-4-president:u $1)
                (p_EVENT.ENTITY_arg0:b $1 $1))
              (p_EMPTY:u $1))
            (and:c
              (p_TYPE_w-6-us:u $3)
              (p_EVENT_w-6-us:u $3)
              (p_EVENT.ENTITY_arg0:b $3 $3))
            (p_EVENT.ENTITY_l-nmod.w-5-of:b $1 $3))))
      (p_EVENT.ENTITY_arg1:b $0 $1))))
```
Here I prefer having non-entity as my event predicate. So the cleaned predicates look like
```
visit.arg1(7:e , 0:m.trump), president.arg0(3:e , 0:m.trump), 
president.nmod.of(3:e , 5:m.us), visit.arg2(7:e , 8:m.uk), 
president(3:s , 0:m.trump)
```

  
