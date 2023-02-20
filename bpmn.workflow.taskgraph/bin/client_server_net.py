#
# Copyright (c) 2021 Contributors to the Eclipse Foundation
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

from nets import PetriNet, Place, Transition, Value, Variable, Expression
from typing import Set, Any, List, Optional, Dict
import re, random, json
if not 'ReachabilityGraph' in globals():
    from reachability_graph import ReachabilityGraph, Node, Edge, Path

class Message:
    def __init__(self, id: int, args: str):
        self.id = id
        self.args = args

    def __repr__(self):
        return f"'{self.args}'"

class Queue:
    def __init__(self, ids: Dict[str, List[int]], in_queue: Dict[str, int], client_cid: int, client_pid: int, server_cid: int, server_pid: int):
        self.ids = ids 
        self.in_queue = in_queue 
        self.client_cid = client_cid 
        self.client_pid = client_pid 
        self.server_cid = server_cid 
        self.server_pid = server_pid 

    def p(self, event: str, side: str):
        ids = json.loads(json.dumps(self.ids))
        in_queue = self.in_queue.copy()
        pid = (self.client_pid if side == 'client' else self.server_pid) + 1
        if not event in ids: 
            ids[event] = [pid]
            in_queue[event] = 1
        else: 
            ids[event].append(pid)
            in_queue[event] += 1
        client_pid = self.client_pid + 1 if side == 'client' else self.client_pid
        server_pid = self.server_pid + 1 if side == 'server' else self.server_pid
        return Queue(ids, in_queue, self.client_cid, client_pid, self.server_cid, server_pid)

    def msg(self, side: str, args: str):
        pid = (self.client_pid if side == 'client' else self.server_pid) + 1
        return Message(pid, args)

    def size(self, event: str):
        return self.in_queue[event] if event in self.in_queue else 0

    def c(self, event: str, side: str, id: int):
        ids = json.loads(json.dumps(self.ids))
        in_queue = self.in_queue.copy()
        in_queue[event] -= 1
        client_cid = id if side == 'client' else self.client_cid
        server_cid = id if side == 'server' else self.server_cid
        return Queue(ids, in_queue, client_cid, self.client_pid, server_cid, self.server_pid)

    def next(self, side: str, event: str):
        cid = self.client_cid if side == 'client' else self.server_cid
        return min([c for c in self.ids[event] if c > cid], default=-1)

    def __repr__(self):
        return "''"

    def __hash__(self):
        return hash((id(self.ids), self.client_cid, self.client_pid, self.server_cid, self.server_pid))

class ClientServerNet:
    seen_edges: Set['Edge'] = set()
    transition_names: Dict[str, Dict[str, Any]] = {}

    def __init__(self, rg: 'ReachabilityGraph', queue_size: int):
        self.rg = rg
        self.queue_size = queue_size
        self.net = PetriNet("N")
        self.add_place('queue', [Queue({}, {}, 0, 0, 0, 0)], {'type': 'queue'})
        self.walk_recursive(rg.initial, rg.initial)
        self.initial_marking = self.net.get_marking()

    def get_transition_name(self, edge: 'Edge') -> str:
        key = {'source': edge.source, 'target': edge.target, 'transition': edge.transition}
        name = next((e[0] for e in self.transition_names.items() if e[1] == key), None)
        if not name:
            base_name = edge.transition.name
            name = base_name
            count = 0
            while name in self.transition_names:
                name = f"{count}_{base_name}"
                count += 1

            self.transition_names[name] = key

        return name

    def add_transition(self, edge: 'Edge', from_place: str, to_place: str, side: str, event_place: Optional[str]):
        arguments = None
        if edge.has_event:
            match = re.match(r'^.+(\(.*\))$', edge.event().evaluated)
            assert match != None
            arguments = match[1][1:][:-1]

        name = f"{side}_{self.get_transition_name(edge)}"
        transition = next((t for t in self.net.transition() if t.name == name), None)
        if transition == None:
            expression = None
            if edge.has_event:
                if edge.direction != side:
                    expression = Expression(f"msg.args == \"{arguments}\" and msg.id == q.next('{side}', '{edge.event().name}')")
                else:
                    expression = Expression(f"q.size('{edge.event().name}') < {self.queue_size}")

            transition = Transition(name, expression)
            self.net.add_transition(transition)
            if edge.has_event:
                transition.meta = {'type': 'event', 'event': {'name': edge.event().name, 'type': edge.event().type, 'evaluated': edge.event().evaluated}, 'side': side}
            else:
                transition.meta = {'type': 'none', 'side': side}

        self.net.add_input(from_place, name, Value(''))
        self.net.add_output(to_place, name, Value(''))

        if event_place != None:
            self.net.add_input('queue', name, Variable('q'))
            if edge.direction == side:
                label = Expression(f"q.p('{edge.event().name}', '{side}')")
            else:
                label = Expression(f"q.c('{edge.event().name}', '{side}', msg.id)")
            self.net.add_output('queue', name, label)
            if edge.direction == side:
                label = Expression(f"q.msg('{side}', \"{arguments}\")")
                self.net.add_output(event_place, name, label)
            else: 
                self.net.add_input(event_place, name, Variable('msg'))

    def add_place(self, name: str, token: List[Any], meta: Dict[str, str]):
        if not any(p.name == name for p in self.net.place()):
            place = Place(name, token)
            place.meta = meta
            self.net.add_place(place)

    def to_place_meta(self, node: 'Node', side: str) -> Dict[str, str]:
        if 'transition' in node.place_types:
            return {'type': 'transition', 'machine': side}
        elif 'clause' in node.place_types:
            return {'type': 'clause', 'machine': side}
        elif 'state' in node.place_types:
            return {'type': 'state', 'state': node.states_str(), 'machine': side}
        assert False

    def walk_recursive(self, source: 'Node', node: 'Node'):
        for edge in node.outgoing:
            if not edge in self.seen_edges:
                self.seen_edges.add(edge)

                if 'transition' in edge.source.place_types and not any(e for e in edge.target.outgoing if e.target == edge.target):
                    # Skip first transition to clause place transition, this breaks OR
                    # and the transition is always transient anyway.
                    self.walk_recursive(source, edge.target)
                else:
                    event_place = None
                    if edge.has_event:
                        event_place = f"middelware_{edge.event().name}"
                        self.add_place(event_place, [], {'type': 'middelware'})

                    for side in ['client', 'server']:
                        from_place = f"{side}_{source.id}"
                        to_place = f"{side}_{edge.target.id}"
                        self.add_place(from_place, [''] if self.rg.initial == node else [], self.to_place_meta(source, side)) 
                        self.add_place(to_place, [], self.to_place_meta(edge.target, side))
                        self.add_transition(edge, from_place, to_place, side, event_place)
                        
                    self.walk_recursive(edge.target, edge.target)

    def serialize(self) -> str:
        self.net.set_marking(self.initial_marking)
        text = "Transitions\n"
        for t in self.net.transition(): 
            text += "%s\ninput: %s\noutput: %s\n\n" % (t.__repr__(), t.input(), t.output())

        text += "Places\n"
        for p in self.net.place():
            text += "%s\n" % p.__repr__()

        return text

    def play(self, max_depth:int) -> None:
        depth = 0
        size = 25
        print ("{:<{size}}{:<{size}}{}".format("client", "middelware", "server", size=size))
        while True:
            transitions = [t for t in self.net.transition() if len(t.modes()) > 0]
            if len(transitions) == 0: 
                print(f'{"=" * size} DEADLOCK {"=" * size}')
                break
            transition = random.choice(transitions)
            mode = random.choice(transition.modes())
            assert transition.edge != None
            if transition.edge.has_event:
                arrow = '->' if transition.edge.event().type in ['command', 'signal'] else '<-'
                side = transition.name.split('_')[0]
                if side == 'client':
                    print ("{:>{size}} {}|".format(transition.edge.event().evaluated, arrow, size=size))
                else:
                    print ("{:<{size}}|{} {}".format('', arrow, transition.edge.event().evaluated, size=size + 3))

            if transition.edge.target.is_state:
                if transition.name.startswith("client_"):
                    print ("{:<{size}}|".format(transition.edge.target.states_str(), size=size + 3))
                else:
                    print ("{:<{size}}|{:>{size}}".format('', transition.edge.target.states_str(), size=size + 3))

            transition.fire(mode)
            depth += 1
            if depth > max_depth:
                print(f'{"=" * size} MAX DEPTH REACHED {"=" * size}')
                break

    def build_rg(self):
        self.rg = ReachabilityGraph(self.net, None, 'shortest')

    def print_path(self, path: 'Path'):
        size = 25
        print ("{:<{size}}{:<{size}}{}".format("client", "middelware", "server", size=size))
        print ("{:<{size}}|".format(path.edges[0].source.states['client'], size=size + 3))
        print ("{:<{size}}|{:>{size}}".format('', path.edges[0].source.states['server'], size=size + 3))
        for edge in path.edges:
            side = edge.transition.name.split('_')[0]
            if edge.has_event:
                arrow = '->' if edge.event().type in ['command', 'signal'] else '<-'
                if side == 'client':
                    print ("{:>{size}} {}|".format(edge.event().evaluated, arrow, size=size))
                else:
                    print ("{:<{size}}|{} {}".format('', arrow, edge.event().evaluated, size=size + 3))
            if side in edge.target.states:
                state = edge.target.states[side]
                if side == 'client':
                    print ("{:<{size}}|".format(state, size=size + 3))
                else:
                    print ("{:<{size}}|{:>{size}}".format('', state, size=size + 3))

    def weak_termination_check(self) -> List['Node']:
        # from a node you should be able to reach a node where client and server are in the same state and middelware is empty
        def place_name(node: 'Node', text: str) -> str:
            return next(p.name[len(text):] for p in node.places if p.name.startswith(text))

        home_states = [n for n in self.rg.nodes.values()
            if n.is_state and not 'middelware' in n.place_types and place_name(n, 'client_') == place_name(n, 'server_')]

        valid_state_nodes: List['Node'] = []
        invalid_nodes: List['Node'] = []
        for n in self.rg.state_nodes:
            if n in home_states or any(next(n.find_paths('node', h), None) != None for h in home_states):
                valid_state_nodes.append(n)
            else:
                invalid_nodes.append(n)

        for n in self.rg.nodes.values():
            if not n.is_state and not any(n in valid_state_nodes for n in n.next_state_nodes):
                invalid_nodes.append(n)

        # Remove all invalid nodes that come before another invalid node
        problem_nodes = [n for n in invalid_nodes if not any(e for e in n.outgoing if e.target != n and e.target in invalid_nodes)]
        return problem_nodes
