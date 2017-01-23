#Brief guide to simplified logical form

Consider the utterance:

    Pixar, the company which Disney acquired, made Ratatouille.

DepLambda will produce the following neo-Davidsonian representation:

    (lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (exists:ex $2:<a,e> (and:c (p_EVENT_w-9-made:u $0) 
    (p_TYPE_w-10-ratatouille:u $2 (p_EVENT.ENTITY_arg2:b $0 $2))) (and:c (and:c (p_TYPE_w-1-pixar:u $1) 
    (p_EVENT_w-1-pixar:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (and:c (and:c (and:c (p_TYPE_w-4-company:u $1) 
    (p_EVENT_w-4-company:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EMPTY:u $1))
    (exists:<<a,e>,<t,t>> $3:<a,e> (exists:ex $4:<a,e> (and:c (exists:ex $5:<a,e> (and:c (exists:ex $6:<a,e> 
    (and:c (p_EVENT_w-7-acquired:u $3) (p_EMPTY:u $6) (p_EVENT.ENTITY_arg2:b $3 $6))) 
    (p_EQUAL:<<a,e>,<<a,e>,t>> $1 $5) (p_EVENT.ENTITY_arg2:b $3 $5))) (p_TYPE_w-6-disney:u $4) 
    (p_EVENT.ENTITY_arg1:b $3 $4)))))) (p_EVENT.ENTITY_arg1:b $0 $1))))

This can be a bit hard to read -- new existential variables are introduced (`$1`, `$2`, etc), the
variables (and predicate symbols) are adorned with type information, 
predicate names are complex (constructed, e.g., from kind information such as `EVENT.ENTITY`
and the actual predicate name, `arg0`). Further, there are equality constraints (`(p_EQUAL:<<a,e>,<<a,e>,t>> $1 $5)`) 
which should really be simplified away, and predications arising from the way in which the formula was constructed
(`(p_EMPTY:u $6)`) which have no logical import and should be eliminated.

DepLambda takes care of these issues. It walks the form above and produces the following simplified 
conjunction of atomic formulas:

    [["company(3:e)","made(8:e)","arg1(6:e , 5:m.disney)","pixar(3:e)","arg2(8:e , 9:m.ratatouille)",
    "acquired(6:e)","arg2(6:e , 0:m.pixar)","arg0(3:e , 0:m.pixar)","company(3:s , 0:m.pixar)",
    "arg1(8:e , 0:m.pixar)"]]

Here is what is going on. The neo-Davidsonian representation typically starts 
with a lambda expression, following by a logical formula that consists of 
interleaved existentials and conjunctions of unary and binary atomic formulas. 
The simplified logical form gets rid of the existentials by exploiting the 
idea that each variable typically occurs as the sole argument of a "defininig" 
predication, which is also typically associated with a word in the utterance. Hence
each variable occurrence can be replaced by a Skolem constant whose name carries the index (minus 1) of the word
in the utterance it stands for, and the type of this occurrence. The type (`:x`, `:s`, `:e`, `:m`) 
is obtained from the kind information in the predication in which the variable has the "defining" occurrence,
and the predication in which this constant occurs. (Recall that in DepLambda variables take as values pairs
of individuals, one of type "event" and one of type "individual" and the context of use is intended to determine
which of these is implicitly selected.)

Thus:
*  `3:e` is  the constant `3` of type `event` ... the denotation of the word `company` 
in the utterance -- note the index is `4` (3+1) since the comma is counted
* `0:m.pixar` is the constant `0` which stands for the named entity `pixar` 
in the utterance (index `1`)
* Thought not ilustrated here, `11:x` would stand for the constant `11` of 
type `individual` that is not a named entity.
* `3:s` is the "type" associated with constant `3` ... see below for how such a term is used.

Now we can read the atomic formulas in the simplified logical form above:
* `company(3:e)` asserts that there is an event `3` which is a "being a company" 
event. This is a consequence of the predication `(p_EVENT_w-4-company:u $1)`
in the lambda expression. Similarly for `made(8:e)`,`pixar(3:e)` and `acquired(6:e)`. 
* `arg1(6:e,5:m.disney)` asserts that the `arg1` of `6:e` ("acquired") is 
`5:m:disney` ("Disney"). This is a consequence of the predications
`(p_EVENT.ENTITY_arg1:b $3 $4)`, `(p_EVENT_w-7-acquired:u $3)` and `(p_TYPE_w-6-disney:u $4)`
in the lambda form. Similarly for `arg2(8:e,9:m.ratatouille)`, `arg2(6:e,0:m.pixar)`,`arg0(3:e,0:m.pixar)` 
and `arg1(8:e,0:m.pixar)`.
* `company(3:s,0:m.pixar)` asserts that constant `0` (i.e. "pixar") is a company
(represented by index `3`). This is a consequence of `(p_EVENT_w-1-pixar:u $1)` and `(p_TYPE_w-4-company:u $1)`
in the lambda form.

Thus we can paraphrase the simplified logical form as: there is a company event 
(`3:e`), a make event (`8:e`), a pixar event (`3:e`), an acquired event (`6:e`).
Further, Disney is the acquirer (`arg1(6:e, 5:m.disney)`) and Pixar the acquired (`arg2(6:e,0:m.pixar)`), 
Ratatouille was made (`arg2(8:e,9:m.ratatouille)`) by Pixar (`arg1(8:e,0:m.pixar)`), 
and Pixar (the named entity) participates in the Pixar event (`arg0(3;e, 0:m.pixar)`).

It takes a little bit of practice to start reading this, but this notation
is certainly simpler and more compact than the unvarnished neo-Davidsonian 
representation!
