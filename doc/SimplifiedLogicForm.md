#Brief guide to simplified logical form

Consider the utterance:
```Pixar, the company which Disney acquired, made Ratatouille.```

DepLambda will produce the following neo-Davidsonian representation
```
(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-9-made:u $0) (p_TYPE_w-10-ratatouille:u $2) (p_EVENT.ENTITY_arg2:b $0 $2))) (and:c (and:c (p_TYPE_w-1-pixar:u $1) (p_EVENT_w-1-pixar:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (and:c (and:c (and:c (p_TYPE_w-4-company:u $1) (p_EVENT_w-4-company:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EMPTY:u $1)) (exists:<<a,e>,<t,t>> $3:<a,e> (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (exists:ex $6:<a,e> (and:c (p_EVENT_w-7-acquired:u $3) (p_EMPTY:u $6) (p_EVENT.ENTITY_arg2:b $3 $6))) (p_EQUAL:<<a,e>,<<a,e>,t>> $1 $5) (p_EVENT.ENTITY_arg2:b $3 $5))) (p_TYPE_w-6-disney:u $4) (p_EVENT.ENTITY_arg1:b $3 $4)))))) (p_EVENT.ENTITY_arg1:b $0 $1))))```

This can be a bit hard to read -- predicate symbols are adorned with type information,
predicate names are complex: constructed from kind information (e.g. `EVENT.ENTITY`),
and the actual predicate name (`arg0`). 
DepLambda also produces the following simplified logical form:
```
[["company(3:e)","made(8:e)","arg1(6:e , 5:m.disney)","pixar(3:e)","arg2(8:e , 9:m.ratatouille)","acquired(6:e)","arg2(6:e , 0:m.pixar)","arg0(3:e , 0:m.pixar)","company(3:s , 0:m.pixar)","arg1(8:e , 0:m.pixar)"]]
```

Here is what is going on. The neo-Davidsonian representation typically starts 
with a lambda expression, following by a logical formula that consists of 
interleaved existentials and conjunctions of unary and binary atomic formulas. 
The simplified logical form gets rid of the existential prefix by replacing 
variables with Skolem constants whose names carry the index (minus 1) of the word
in the utterance they stand for, and their types. The type (`:x`, `:s`, `:e`, `:m`) 
is obtained from the kind information in predication in which the variable
has the "defining" occurrence.

Thus:
*  `3:e` is  the constant `3` of type `event` ...the denotation of the word `company` 
in the utterance -- note the index is `3` since the comma is counted (3=4-1), 
* `0:m.pixar` is the constant `0` which stands for the named entity `pixar` 
in the utterance (index 1)
* Thought not ilustrated here, `11:x` would stand for the constant `11` of 
type `individual` that is not a named entity.
* `3:s` is the "type" associated with constant `3` ... see below for how such a term is used.

Now we can read the atomic formulas in the simplified logical form above:
* `company(3:e)` asserts that there is an event `3` which is a "being a company" 
event. Similarly for `made(8:e)`,`pixar(3:e)` and `acquired(6:e)`.
* `arg1(6:e,5:m.disney)` asserts that the `arg1` of `6:e` ("acquired") is 
`5:m:disney` ("Disney"). Similarly for `arg2(8:e,9:m.ratatouille)`, 
`arg2(6:e,0:m.pixar)`,`arg0(3:e,0:m.pixar)` and `arg1(8:e,0:m.pixar)`.
* `company(3:s,0:m.pixar)` asserts that constant `0` (i.e. "pixar") is a company
(represented by index `3`). 

Thus we can paraphrase the simplified logical form as: there is a company event 
(`3:e`), a make event (`8:e`), a pixar event (`3:e`), an acquired event (`6:e`).
Further, Disney is the acquirer (`arg1(6:e, 5:m.disney)`) and Pixar the acquired (`arg2(6:e,0:m.pixar)`), Ratatouille was made (`arg2(8:e,9:m.ratatouille)`) by Pixar (`arg1(8:e,0:m.pixar)`), and Pixar the named entity participates in the Pixar 
event (`arg0(3;e, 0:m.pixar)`).

It takes a little bit of practice to start reading this, but this notation
is certainly simpler and more compact than the unvarnished neo-Davidsonian 
representation!
