package pl.devthoughts.todos.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import javaslang.control.Option;
import pl.devthoughts.todos.domain.TodoItem;
import pl.devthoughts.todos.domain.TodoItemId;
import pl.devthoughts.todos.domain.TodoItems;
import pl.devthoughts.todos.repository.TodoItemHashMapRepository;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.function.Function;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Patterns.None;
import static javaslang.Patterns.Some;
import static play.libs.Json.fromJson;
import static play.libs.Json.toJson;

public class TodoController extends Controller {

    private static final Logger.ALogger LOGGER = Logger.of(TodoController.class);

    private final TodoItemHashMapRepository repository = new TodoItemHashMapRepository();

    public TodoController() {

    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result addItem() {
        TodoItemRequest req = getRequest();
        TodoItemId itemId = repository.saveItem(TodoItem.fromRequest(req));
        LOGGER.info("Item {} has been created with id {}", req.getName(), itemId.getId());
        return created(toJson(itemId));
    }

    public Result getItem(String id) {
        return doCall
            (id, (TodoItem it) -> {
                LOGGER.info("Returning item {}", id);
                return ok(toJson(it));
            });
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result updateItem(String id) {
        final TodoItemRequest req = getRequest();
        return doCall
            (id, (TodoItem it) -> {
                repository.updateItem(it, req);
                    LOGGER.info("Item {} has been updated", id);
                    return ok();
                }
            );
    }

    public Result deleteItem(String id) {
        return doCall
            (id, (TodoItem it) -> {
                repository.removeItem(it);
                    LOGGER.info("Item {} has been removed", id);
                    return ok();
                }
            );
    }

    public Result done(String id) {
        return doCall
            (id, (TodoItem it) -> {
                repository.finishItem(it);
                LOGGER.info("Item {} changed status to done", id);
                    return ok();
                }
            );
    }

    public Result reopen(String id) {
        return doCall
            (id, (TodoItem it) -> {
                repository.reopenItem(it);
                LOGGER.info("Item {} changed status to open", id);
                    return ok();
                }
            );
    }

    public Result getAllItems() {
        TodoItems items = repository.findAllItems();
        return ok(toJson(items));
    }

    private Result doCall(String id, Function<TodoItem, Result> operation) {
        Option<TodoItem> item = repository.findItem(id);
        return Match(item).of(
            Case(Some($()), operation),
            Case(None(), () -> {
                LOGGER.warn("Cannot find an item with id {}", id);
                return notFound();
            })
        );
    }

    private TodoItemRequest getRequest() {
        JsonNode json = request().body().asJson();
        return fromJson(json, TodoItemRequest.class);
    }

}
